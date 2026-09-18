package uz.falconmobile.bilim_scan.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.falconmobile.bilim_scan.user.dto.UserCreateDto;
import uz.falconmobile.bilim_scan.user.dto.UserResponseDto;
import uz.falconmobile.bilim_scan.user.dto.UserUpdateDto;
import uz.falconmobile.bilim_scan.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponseDto> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserResponseDto getUserById(@PathVariable String id) {
        return userService.getUserById(id);
    }

    @PostMapping
    public UserResponseDto createUser(@RequestBody UserCreateDto userCreateDto) {
        return userService.createUser(userCreateDto);
    }

    @PutMapping("/{id}")
    public UserResponseDto updateUser(@PathVariable String id, @RequestBody UserUpdateDto userUpdateDto) {
        return userService.updateUser(id, userUpdateDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(true);
    }
}