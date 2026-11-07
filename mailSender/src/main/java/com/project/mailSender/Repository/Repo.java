package com.project.mailSender.Repository;

import com.project.mailSender.Model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Repo extends JpaRepository<Users,Long> {

    List<Users> findByGender(String gender);

    Users findByEmail(String email);

    List<Users> findByCountry(String country);

    List<Users> findByDeletedFalse();

    List<Users> findByDeletedTrueOrderByDeletedAtDesc();

    boolean existsByEmailIgnoreCase(String email);


    @Query("SELECT u FROM Users u WHERE u.deleted = true")
    List<Users> getTrash();
}
