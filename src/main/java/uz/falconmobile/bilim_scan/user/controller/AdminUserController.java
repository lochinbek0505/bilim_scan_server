package uz.falconmobile.bilim_scan.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.falconmobile.bilim_scan.user.dto.UserCreateDto;
import uz.falconmobile.bilim_scan.user.service.UserService;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public String createUser(@RequestBody UserCreateDto userCreateDto) {
        return userService.createUser(userCreateDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return "Foydalanuvchi o'chirildi!";
    }
}