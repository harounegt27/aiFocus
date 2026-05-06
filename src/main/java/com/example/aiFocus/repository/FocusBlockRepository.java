package com.example.aiFocus.repository;

import com.example.aiFocus.entity.BlockStatus;
import com.example.aiFocus.entity.FocusBlock;
import com.example.aiFocus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FocusBlockRepository extends JpaRepository<FocusBlock, Long> {

    List<FocusBlock> findByUser(User user);

    List<FocusBlock> findByUserAndStatus(User user, BlockStatus status);

    List<FocusBlock> findByUserAndCreatedAtAfter(User user, LocalDateTime since);

    List<FocusBlock> findByStatus(BlockStatus status);

    List<FocusBlock> findByUserAndStatusAndStartTimeBetween(User user, BlockStatus status, LocalDateTime from, LocalDateTime to);
}
