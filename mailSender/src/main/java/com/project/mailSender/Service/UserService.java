package com.project.mailSender.Service;

import com.project.mailSender.Model.UpdateDto;
import com.project.mailSender.Model.UserDto;
import com.project.mailSender.Model.Users;
import com.project.mailSender.Repository.Repo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        users.setPrime(false);
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

        repo.save(users);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
