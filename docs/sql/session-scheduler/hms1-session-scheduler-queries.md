
# HMS1 — Doctor Session Scheduler (SQL) Documentation

This document explains the SQL snippets used in **`hms1`** for fetching **Doctor Override schedules**, fetching the **latest schedule per mode**, truncating scheduler tables safely, and listing **slots** for a doctor in a date range.

---

## Database

- **Schema:** `hms1`

---

## Tables referenced

### Scheduler tables
- `session_schedules` — schedule header (mode, doctor, week start, duration, status, version, lock flags)
- `session_schedule_days` — per-day settings for a schedule (day of week, day off)
- `session_schedule_intervals` — working intervals (start/end/session type/capacity) per day
- `session_schedule_blocks` — block windows (lunch/meeting/custom) per day

### Supporting tables
- `users` — doctor user record
- `specializations` — doctor specialization metadata
- `slots` — appointment slots (date + time ranges), recurring flag, notes, status, etc.

---

## Query 1 — Fetch Doctor Override schedules with nested intervals and blocks (JSON)

### Purpose
Fetch **Doctor Override** schedules for a specific doctor (optionally for a specific week), including:
- schedule header info
- doctor info (name, username, specialization)
- each schedule day
- nested JSON arrays for **intervals** and **blocks** per day  
  ✅ implemented via correlated subqueries so intervals and blocks do not cross-multiply.

### SQL
```sql
SELECT
      ss.id                      AS schedule_id,
      ss.version ,
      ss.week_start_date,
      ss.slot_duration_minutes,
      ss.status,
      ss.locked,
      u.id                       AS doctor_id,
      CONCAT(u.first_name, ' ', u.last_name) AS doctor_name,
      u.username,
      sp.name                    AS specialization,
      d.id                       AS day_id,
      d.day_of_week,
      d.is_day_off,

      /* nested arrays so intervals and blocks don't cross-multiply */
      (SELECT JSON_ARRAYAGG(JSON_OBJECT(
                  'id', i.id,
                  'start_time', i.start_time,
                  'end_time', i.end_time,
                  'session_type', i.session_type,
                  'capacity', i.capacity))
       FROM session_schedule_intervals i
       WHERE i.day_id = d.id) AS intervals,

      (SELECT JSON_ARRAYAGG(JSON_OBJECT(
                  'id', b.id,
                  'block_type', b.block_type,
                  'start_time', b.start_time,
                  'end_time', b.end_time,
                  'reason', b.reason))
       FROM session_schedule_blocks b
       WHERE b.day_id = d.id) AS blocks

  FROM session_schedules ss
  JOIN users u                 ON u.id = ss.doctor_id
  LEFT JOIN specializations sp ON sp.id = u.specialization_id
  LEFT JOIN session_schedule_days d ON d.schedule_id = ss.id

  WHERE ss.mode = 'DOCTOR_OVERRIDE'
    AND ss.status <> 'ARCHIVED'
    AND ss.doctor_id = 21          -- bind: target doctor
--  AND ss.week_start_date = ?      -- bind: week (Monday) you care about
--    AND u.first_name LIKE '%rahu%'

  ORDER BY ss.week_start_date, d.day_of_week;
````

### Bind parameters (recommended)

* `:doctor_id` → integer doctor id (example: `21`)
* `:week_start_date` → `YYYY-MM-DD` (must be Monday if your domain requires week start as Monday)

### Output shape (conceptual)

One row per `(schedule_id, day_id)` with:

* `intervals` = JSON array of intervals for that day (or `NULL` if none)
* `blocks` = JSON array of blocks for that day (or `NULL` if none)

### Notes / gotchas

* Because `session_schedule_days d` is `LEFT JOIN`, schedules without days may produce `d.* = NULL`.
* In MySQL, `JSON_ARRAYAGG()` returns `NULL` when there are no rows. If you need empty arrays instead, wrap with `COALESCE(..., JSON_ARRAY())`.
* The line `AND d.schedule_id` in your pasted query looks incomplete and should be removed or fixed (example: `AND d.schedule_id = ss.id`), but it is already guaranteed by the join condition.

---

## Query 2 — Fetch latest schedule per mode (JSON days aggregation)

### Purpose

Return the **latest** schedule **per mode** using window function ranking:

* `ROW_NUMBER() OVER (PARTITION BY ss.mode ORDER BY ss.version DESC, ss.id DESC)`
* picks `rn = 1` → latest schedule for each mode
  Then attaches a single `days_json` array where each element contains:
* day metadata
* nested intervals array
* nested blocks array

### SQL

```sql
WITH base AS (
      SELECT
          ss.*,
          ROW_NUMBER() OVER (
              PARTITION BY ss.mode
              ORDER BY ss.version DESC, ss.id DESC
          ) AS rn
      FROM session_schedules ss
      WHERE ss.status <> 'ARCHIVED'

-- Optional filter logic (example)
-- AND (
--       (:doctor_id IS NULL  AND ss.mode = 'DOCTOR_OVERRIDE')
--    OR (:doctor_id IS NOT NULL AND ss.doctor_id = :doctor_id)
-- )
),
days AS (
      SELECT
          d.schedule_id,
          JSON_ARRAYAGG(
              JSON_OBJECT(
                  'dayId', d.id,
                  'dayOfWeek', d.day_of_week,
                  'dayOff', d.is_day_off,
                  'intervals', (
                      SELECT JSON_ARRAYAGG(JSON_OBJECT(
                          'intervalId', i.id,
                          'start', DATE_FORMAT(i.start_time, '%H:%i'),
                          'end',   DATE_FORMAT(i.end_time,   '%H:%i'),
                          'sessionType', i.session_type,
                          'capacity', i.capacity
                      ))
                      FROM session_schedule_intervals i
                      WHERE i.day_id = d.id
                  ),
                  'blocks', (
                      SELECT JSON_ARRAYAGG(JSON_OBJECT(
                          'blockId', b.id,
                          'blockType', b.block_type,
                          'start', DATE_FORMAT(b.start_time, '%H:%i'),
                          'end',   DATE_FORMAT(b.end_time,   '%H:%i'),
                          'reason', b.reason
                      ))
                      FROM session_schedule_blocks b
                      WHERE b.day_id = d.id
                  )
              )
          ) AS days_json
      FROM session_schedule_days d
      GROUP BY d.schedule_id
)
SELECT
      b.id                AS schedule_id,
      b.mode,
      b.doctor_id,
      b.department_id,
      b.week_start_date,
      b.slot_duration_minutes,
      b.status,
      b.locked,
      b.locked_reason,
      b.version,
      d.days_json
FROM base b
LEFT JOIN days d ON d.schedule_id = b.id
WHERE b.rn = 1   -- pick latest per mode
ORDER BY b.mode, b.id;
```

### Typical use-cases

* Get the most recent **GLOBAL_TEMPLATE** schedule and most recent **DOCTOR_OVERRIDE** schedule in one call.
* Optionally restrict to a doctor’s latest override (uncomment and bind `:doctor_id`).

### Notes / gotchas

* If you want “latest per doctor per mode”, change `PARTITION BY ss.mode` to:

    * `PARTITION BY ss.mode, ss.doctor_id`
* Similar to Query 1: `JSON_ARRAYAGG()` yields `NULL` when no children exist. Use `COALESCE(..., JSON_ARRAY())` if your API expects empty arrays.

---

## Maintenance — Truncate scheduler tables safely (reset all schedules)

### Purpose

Clear all scheduler data in proper dependency order:

1. blocks
2. intervals
3. days
4. schedules

### SQL

```sql
USE hms1;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE session_schedule_blocks;
TRUNCATE TABLE session_schedule_intervals;
TRUNCATE TABLE session_schedule_days;
TRUNCATE TABLE session_schedules;

SET FOREIGN_KEY_CHECKS = 1;
```

### Warning

* This deletes **all schedules** permanently (no `WHERE` clause).
* Disabling FK checks is powerful—use only in non-prod or controlled maintenance windows.

---

## Query 3 — Fetch `slots` for a doctor in a date range

### Purpose

Return all slot rows for a doctor between two dates (inclusive), ordered by date.

### SQL

```sql
SELECT 
  `date`,
  start_time,
  end_time,
  is_recurring,
  doctor_id,
  id,
  created_by,
  notes,
  updated_by,
  status,
  `type`
FROM hms1.slots
WHERE doctor_id = 2
  AND `date` BETWEEN '2026-02-02' AND '2026-07-02'
ORDER BY date asc;
```

### Bind parameters (recommended)

* `:doctor_id` → integer (example: `2`)
* `:from_date` → `YYYY-MM-DD` (example: `2026-02-02`)
* `:to_date` → `YYYY-MM-DD` (example: `2026-07-02`)

### Notes

* `BETWEEN` is inclusive on both ends.
* If you want time ordering within a day, change:

    * `ORDER BY date asc, start_time asc`

---

## Recommended improvements (optional)

### 1) Return empty arrays instead of NULL

If your API expects consistent JSON arrays:

* Replace:

    * `JSON_ARRAYAGG(...)`
* With:

    * `COALESCE(JSON_ARRAYAGG(...), JSON_ARRAY())`

### 2) Ensure stable day ordering inside JSON_ARRAYAGG

MySQL doesn’t guarantee order unless you enforce it.
If needed, restructure using a subquery that orders days before aggregation.

### 3) Indexes (performance)

For large data sets, typical helpful indexes:

* `session_schedules(mode, doctor_id, status, week_start_date, version)`
* `session_schedule_days(schedule_id, day_of_week)`
* `session_schedule_intervals(day_id, start_time)`
* `session_schedule_blocks(day_id, start_time)`
* `slots(doctor_id, date)`

---

## Appendix — Common “latest schedule” variants

### Latest DOCTOR_OVERRIDE for a doctor (single row)

* Partition by `(mode, doctor_id)` and filter `mode='DOCTOR_OVERRIDE'`.

### Latest schedule for a doctor and a specific week

* Filter with:

    * `AND ss.week_start_date = :week_start_date`

---

```

