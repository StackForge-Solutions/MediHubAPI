package com.MediHubAPI.repository.pharmacy;

import com.MediHubAPI.model.pharmacy.PharmacyPurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;

public interface PharmacyPurchaseOrderItemRepository extends JpaRepository<PharmacyPurchaseOrderItem, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item
            from PharmacyPurchaseOrderItem item
            join fetch item.medicine
            where item.purchaseOrder.id = :purchaseOrderId
              and item.id in :itemIds
            """)
    List<PharmacyPurchaseOrderItem> findAllForReceipt(@Param("purchaseOrderId") Long purchaseOrderId,
                                                      @Param("itemIds") Collection<Long> itemIds);

    @Query("""
            select count(item)
            from PharmacyPurchaseOrderItem item
            where item.purchaseOrder.id = :purchaseOrderId
              and (coalesce(item.receivedQty, 0) < coalesce(item.orderedQty, 0))
            """)
    long countPendingItems(@Param("purchaseOrderId") Long purchaseOrderId);

    @Query("""
            select item
            from PharmacyPurchaseOrderItem item
            join fetch item.medicine
            where item.purchaseOrder.id = :purchaseOrderId
            order by item.id asc
            """)
    List<PharmacyPurchaseOrderItem> findByPurchaseOrderIdWithMedicine(@Param("purchaseOrderId") Long purchaseOrderId);

    void deleteByPurchaseOrder_Id(Long purchaseOrderId);
}
