package com.project.mailSender.Repository;

import com.project.mailSender.Model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Repo extends JpaRepository<Users,Long> {

    List<Users> findByGender(String gender);
}
