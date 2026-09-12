package com.fedstack.spending.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantJpaRepository extends JpaRepository<MerchantEntity, Long> {
}
