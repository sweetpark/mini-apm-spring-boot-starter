package io.github.sweetpark.apm.test.controller;

import io.github.sweetpark.apm.test.entity.UserEntity;
import io.github.sweetpark.apm.test.mapper.TestMapper;
import io.github.sweetpark.apm.test.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class TestController {

    private final TestMapper testMapper;
    private final UserRepository userRepository;

    public TestController(TestMapper testMapper, UserRepository userRepository) {
        this.testMapper = testMapper;
        this.userRepository = userRepository;
    }

    @GetMapping("/test")
    public String test() {
        int result = testMapper.selectOne();
        return "OK: " + result;
    }

    @GetMapping("/test-param")
    public String testParam(@RequestParam("value") String value) {
        String result = testMapper.selectParam(value);
        return "Result with param: " + result;
    }

    @GetMapping("/jpa/users")
    public List<UserEntity> jpaUsers(@RequestParam(value = "name", defaultValue = "testuser") String name) {
        UserEntity user = new UserEntity(name, name + "@sweetpark.io");
        userRepository.save(user);
        return userRepository.findByName(name);
    }

    @GetMapping("/error-api")
    public Map<String, Object> errorApi() {
        return Map.of("resCode", "9999", "message", "Simulated Business Error");
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        return "File uploaded: " + file.getOriginalFilename();
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download() {
        byte[] content = "dummy-binary-data".getBytes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file.bin\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }
}