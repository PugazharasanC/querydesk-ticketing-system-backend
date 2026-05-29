package com.query.ticket.service;

import com.query.ticket.dto.request.CreateTeamRequest;
import com.query.ticket.dto.response.TeamResponse;
import com.query.ticket.model.Team;
import com.query.ticket.model.User;
import com.query.ticket.repository.TeamRepository;
import com.query.ticket.repository.UserRepository;
import com.query.ticket.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream().map(this::toResponse).toList();
    }

    public TeamResponse getTeamById(String id) {
        return toResponse(findTeamById(id));
    }

    public TeamResponse createTeam(CreateTeamRequest request, UserPrincipal currentUser) {
        if (teamRepository.existsByName(request.getName())) {
            throw new RuntimeException("Team with this name already exists");
        }

        Team.TeamBuilder builder = Team.builder()
                .name(request.getName())
                .description(request.getDescription());

        // Set manager
        String managerId = request.getManagerId() != null
                ? request.getManagerId()
                : currentUser.getId();

        userRepository.findById(managerId).ifPresent(manager -> {
            builder.managerId(manager.getId());
            builder.managerName(manager.getName());
        });

        return toResponse(teamRepository.save(builder.build()));
    }

    public TeamResponse addMember(String teamId, String userId) {
        Team team = findTeamById(teamId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!team.getMemberIds().contains(userId)) {
            team.getMemberIds().add(userId);
            team.getMemberNames().add(user.getName());
            user.setTeamId(teamId);
            userRepository.save(user);
        }

        return toResponse(teamRepository.save(team));
    }

    public TeamResponse removeMember(String teamId, String userId) {
        Team team = findTeamById(teamId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        team.getMemberIds().remove(userId);
        team.getMemberNames().remove(user.getName());

        if (teamId.equals(user.getTeamId())) {
            user.setTeamId(null);
            userRepository.save(user);
        }

        return toResponse(teamRepository.save(team));
    }

    public TeamResponse updateTeam(String id, CreateTeamRequest request) {
        Team team = findTeamById(id);

        if (request.getName() != null) team.setName(request.getName());
        if (request.getDescription() != null) team.setDescription(request.getDescription());

        if (request.getManagerId() != null) {
            userRepository.findById(request.getManagerId()).ifPresent(manager -> {
                team.setManagerId(manager.getId());
                team.setManagerName(manager.getName());
            });
        }

        return toResponse(teamRepository.save(team));
    }

    public void deleteTeam(String id) {
        Team team = findTeamById(id);

        // Remove team from all members
        team.getMemberIds().forEach(memberId ->
            userRepository.findById(memberId).ifPresent(user -> {
                user.setTeamId(null);
                userRepository.save(user);
            })
        );

        teamRepository.delete(team);
    }

    private Team findTeamById(String id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found"));
    }

    private TeamResponse toResponse(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .managerId(team.getManagerId())
                .managerName(team.getManagerName())
                .memberIds(team.getMemberIds())
                .memberNames(team.getMemberNames())
                .memberCount(team.getMemberIds().size())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }
}