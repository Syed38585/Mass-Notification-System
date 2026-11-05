package com.project.mailSender.Service;


import com.project.mailSender.Model.AdminModel;
import com.project.mailSender.Repository.AdminRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class AdminDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    public AdminDetailsService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {

        AdminModel admin = adminRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Admin not found"));

        return User.withUsername(admin.getEmail())
                .password(admin.getPassword())
                .roles("ADMIN")
                .disabled(!admin.isEnabled())
                .build();
    }
}