package com.smartscheduler.common.repository;

import com.smartscheduler.common.entity.SlotCancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlotCancellationRepository extends JpaRepository<SlotCancellation, Long> {}

