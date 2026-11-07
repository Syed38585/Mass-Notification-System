package com.project.mailSender.Service;

import com.project.mailSender.Model.Users;
import com.project.mailSender.Repository.Repo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserTrashService {

    private final Repo repo;

    public UserTrashService(Repo repo) {
        this.repo = repo;
    }

    // Get all deleted users
    public List<Users> getTrash() {
        return repo.findByDeletedTrueOrderByDeletedAtDesc();
    }

    // Soft delete
    @Transactional
    public Users moveToTrash(Long id) {

        Users user = repo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (user.isDeleted()) {
            throw new RuntimeException("User is already in trash");
        }

        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());

        return repo.save(user);
    }

    // Restore user
    @Transactional
    public Users restore(Long id) {

        Users user = repo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (!user.isDeleted()) {
            throw new RuntimeException("User is not in trash");
        }

        user.setDeleted(false);
        user.setDeletedAt(null);

        return repo.save(user);
    }

    // Permanent deletion
    @Transactional
    public void permanentlyDelete(Long id) {

        Users user = repo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (!user.isDeleted()) {
            throw new RuntimeException(
                    "Move the user to trash before permanent deletion"
            );
        }

        repo.delete(user);
    }
}