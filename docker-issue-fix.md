Your MySQL container is restarting because your **volume data was created by MySQL 9.5.0**, but now you’re running **MySQL 8.0.44**. MySQL **does not allow downgrade** like this, so it aborts on startup.

### Fix (keep your existing DB data): run MySQL 9.5 with the same volume

Update `docker-compose.yml`:

```yaml
services:
  mysql:
    image: mysql:9.5.0
    container_name: mysql-container
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: Chetan@123
      MYSQL_DATABASE: hms1
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - medihub-net

volumes:
  mysql_data:
    external: true
    name: medihub_mysql_data

networks:
  medihub-net:
    driver: bridge
```

Then run:

```bash
docker compose down
docker compose up -d
docker ps -a --filter name=mysql-container
docker logs --tail 50 mysql-container
```

Now this should work:

```bash
docker exec -it mysql-container mysql -uroot -pChetan@123 -e "SHOW DATABASES;"
```

---

### Why this happened (so it won’t repeat)

If you used `mysql:latest` earlier, Docker might have pulled **MySQL 9.5** at that time (latest “innovation” release), created the data files in the volume, and later you changed image to `mysql:8.0` → downgrade error. ([GitHub][1])

**Best practice:** always pin a version (like `mysql:9.5.0` or `mysql:8.0.44`) instead of `latest`. ([GitHub][1])

---

If you *must* run MySQL 8.0, tell me and I’ll give the exact “dump from 9.5 → import into fresh 8.0 volume” commands (that’s the proper way to downgrade).

[1]: https://github.com/docker-library/official-images/blob/master/library/mysql?utm_source=chatgpt.com "mysql - docker-library/official-images"
