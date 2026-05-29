package com.query.ticket.repository;

import com.query.ticket.model.Team;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends MongoRepository<Team, String> {
    List<Team> findByManagerId(String managerId);
    List<Team> findByMemberIdsContaining(String memberId);
    boolean existsByName(String name);
}