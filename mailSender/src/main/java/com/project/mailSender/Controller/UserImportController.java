package com.project.mailSender.Controller;

import com.project.mailSender.Service.CsvUserImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/user/import")
public class UserImportController {
    private final CsvUserImportService importService;
    public UserImportController(CsvUserImportService importService) { this.importService = importService; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importUsers(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importService.importCsv(file));
    }

    @GetMapping("/sample")
    public ResponseEntity<String> sampleCsv() {
        String csv = "name,email,gender,country,age,dob\\nAisha Khan,aisha@example.com,female,India,24,2002-04-18\\n";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header("Content-Disposition", "attachment; filename=mailflow-users-sample.csv")
                .body(csv);
    }
}
