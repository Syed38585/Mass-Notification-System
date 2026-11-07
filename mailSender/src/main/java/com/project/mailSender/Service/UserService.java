package com.project.mailSender.Service;

import com.project.mailSender.Model.UpdateDto;
import com.project.mailSender.Model.UserDto;
import com.project.mailSender.Model.Users;
import com.project.mailSender.Repository.Repo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    @Autowired
    Repo repo;

    public List<Users> getAll() {
        return repo.findAll();
    }

    public String create(UserDto user) {
        Users users = new Users();
        users.setName(user.getName().toLowerCase());
        users.setEmail(user.getEmail().toLowerCase());
        users.setGender(user.getGender().toLowerCase());
        users.setDob(user.getDob());
        users.setAge(user.getAge());
        users.setCountry(user.getCountry());
        users.setSubscribed(true);
        users.setCreatedAt(LocalDate.now());
        repo.save(users);
         return "DONE";
    }

    public List<Users> sendByGender(String gender) {
        return repo.findByGender(gender);
    }

    public void update(Long id,UpdateDto updateDto) {
        Users users = repo.findById(id).orElseThrow(() -> new RuntimeException("NULL"));

        if(updateDto.getEmail()!=null){
            users.setEmail(updateDto.getEmail().toLowerCase());
        }

        if(updateDto.getName()!=null){
            users.setName(updateDto.getName().toLowerCase());
        }

        if(updateDto.getGender()!=null){
            users.setGender(updateDto.getGender().toLowerCase());
        }

        if (updateDto.getAge() != null){
            users.setAge(updateDto.getAge());
        }

        repo.save(users);
    }

    public Users findByEmail(String email) {
        return repo.findByEmail(email);
    }

    public List<Users> getUsersByIds(List<Long> ids) {
        return repo.findAllById(ids);

    }

    public String delete(Long id) {

        Users user = repo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());

        repo.save(user);

        return "User moved to trash successfully";
    }


    public String unsubscribe(Long id) {
        Users users = repo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        users.setSubscribed(false);
        repo.save(users);

        return "SUCCESSFULLY UNSUBSCRIBED";
    }

    public String updateStatus(Long id) {
        Users users = repo.findById(id).orElseThrow(() ->
                new RuntimeException("User not found in DB"));

            users.setSubscribed(true);
            repo.save(users);
            return "User Subscribed successfully";
    }

    public String updateStatusDisable(Long id) {
        Users users = repo.findById(id).orElseThrow(() ->
                new RuntimeException("User not found in DB"));

        users.setSubscribed(false);
        repo.save(users);
        return "User un-Subscribed successfully";
    }

    public List<Users> getUsersByCountry(String country) {
        return repo.findByCountry(country);
    }

    public List<Users> getTrash() {
        return repo.getTrash();
    }
}
