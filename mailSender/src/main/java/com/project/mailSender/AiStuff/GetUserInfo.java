package com.project.mailSender.AiStuff;

import com.project.mailSender.Model.Users;
import com.project.mailSender.Repository.Repo;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Component
public class GetUserInfo {

    @Autowired
    Repo repo;

    @Tool(description = "Tool used to retrieve information of users")
    public List<Users> getUsers(){
        System.out.println("REPO TOOL HIT");
        return repo.findAll();
    }
}
