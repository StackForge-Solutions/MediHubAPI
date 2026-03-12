package com.MediHubAPI.repository.pharmacy;

import com.MediHubAPI.model.pharmacy.StockAdjustmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockAdjustmentItemRepository extends JpaRepository<StockAdjustmentItem, Long> {

    @Query("""
            select item
            from StockAdjustmentItem item
            join fetch item.medicine
            join fetch item.batch
            where item.stockAdjustment.id = :adjustmentId
            order by item.id asc
            """)
    List<StockAdjustmentItem> findByStockAdjustmentIdWithRelations(@Param("adjustmentId") Long adjustmentId);
}
