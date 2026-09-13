package uz.falconmobile.bilim_scan.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.falconmobile.bilim_scan.catalog.dto.CatalogItemResponseDto;
import uz.falconmobile.bilim_scan.catalog.model.Bosqich;
import uz.falconmobile.bilim_scan.catalog.model.Fan;
import uz.falconmobile.bilim_scan.catalog.model.Guruh;
import uz.falconmobile.bilim_scan.catalog.model.Kafedra;
import uz.falconmobile.bilim_scan.catalog.repository.BosqichRepository;
import uz.falconmobile.bilim_scan.catalog.repository.FanRepository;
import uz.falconmobile.bilim_scan.catalog.repository.GuruhRepository;
import uz.falconmobile.bilim_scan.catalog.repository.KafedraRepository;
import uz.falconmobile.bilim_scan.user.dto.UserCreateDto;
import uz.falconmobile.bilim_scan.user.dto.UserResponseDto;
import uz.falconmobile.bilim_scan.user.dto.UserUpdateDto;
import uz.falconmobile.bilim_scan.user.model.Role;
import uz.falconmobile.bilim_scan.user.model.User;
import uz.falconmobile.bilim_scan.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BosqichRepository bosqichRepository;
    private final GuruhRepository guruhRepository;
    private final KafedraRepository kafedraRepository;
    private final FanRepository fanRepository;

    public UserResponseDto createUser(UserCreateDto dto) {
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new RuntimeException("Foydalanuvchi allaqachon mavjud: " + dto.getUsername());
        }

        User user = new User();
        user.setUsername(requireNonBlank(dto.getUsername(), "Username bo'sh bo'lishi mumkin emas"));
        user.setPassword(passwordEncoder.encode(requireNonBlank(dto.getPassword(), "Parol bo'sh bo'lishi mumkin emas"))); // Parolni shifrlash
        user.setRole(requireRole(dto.getRole()));
        user.setFirstName(requireNonBlank(dto.getFirstName(), "Ism bo'sh bo'lishi mumkin emas"));
        user.setLastName(requireNonBlank(dto.getLastName(), "Familiya bo'sh bo'lishi mumkin emas"));
        user.setPatronymic(requireNonBlank(dto.getPatronymic(), "Sharif bo'sh bo'lishi mumkin emas"));
        user.setProfileImageUrl(requireNonBlank(dto.getProfileImageUrl(), "Profile rasmi linki bo'sh bo'lishi mumkin emas"));
        applyRoleSpecificFields(
                user,
                dto.getRole(),
                dto.getBosqichId(),
                dto.getGuruhId(),
                dto.getKafedraId(),
                dto.getFanId()
        );

        return toResponse(userRepository.save(user));
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponseDto getUserById(String id) {
        return toResponse(findByIdOrThrow(id));
    }

    public UserResponseDto updateUser(String id, UserUpdateDto dto) {
        User user = findByIdOrThrow(id);
        Role role = requireRole(dto.getRole());


        user.setRole(role);
        user.setFirstName(requireNonBlank(dto.getFirstName(), "Ism bo'sh bo'lishi mumkin emas"));
        user.setLastName(requireNonBlank(dto.getLastName(), "Familiya bo'sh bo'lishi mumkin emas"));
        user.setPatronymic(requireNonBlank(dto.getPatronymic(), "Sharif bo'sh bo'lishi mumkin emas"));
        user.setProfileImageUrl(requireNonBlank(dto.getProfileImageUrl(), "Profile rasmi linki bo'sh bo'lishi mumkin emas"));



        // Parolni yangilash
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        applyRoleSpecificFields(user, role, dto.getBosqichId(), dto.getGuruhId(), dto.getKafedraId(), dto.getFanId());
        return toResponse(userRepository.save(user));
    }

    public void deleteUser(String id) {
        findByIdOrThrow(id);
        userRepository.deleteById(id);
    }

    private User findByIdOrThrow(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Foydalanuvchi topilmadi: " + id));
    }

    private Role requireRole(Role role) {
        if (role == null) {
            throw new RuntimeException("Role bo'sh bo'lishi mumkin emas");
        }
        return role;
    }

    private void applyRoleSpecificFields(User user, Role role, String bosqichId, String guruhId, String kafedraId, String fanId) {
        if (role == Role.USER) {
            String normalizedBosqichId = requireNonBlank(bosqichId, "USER uchun bosqich majburiy");
            String normalizedGuruhId = requireNonBlank(guruhId, "USER uchun guruh majburiy");
            if (!bosqichRepository.existsById(normalizedBosqichId)) {
                throw new RuntimeException("Bosqich topilmadi: " + normalizedBosqichId);
            }
            if (!guruhRepository.existsById(normalizedGuruhId)) {
                throw new RuntimeException("Guruh topilmadi: " + normalizedGuruhId);
            }
            Bosqich bosqich = bosqichRepository.findById(normalizedBosqichId)
                    .orElseThrow(() -> new RuntimeException("Bosqich topilmadi: " + normalizedBosqichId));
            CatalogItemResponseDto bosqichDto = CatalogItemResponseDto.builder()
                    .id(bosqich.getId())
                    .name(bosqich.getName())
                    .build();

            Guruh guruh = guruhRepository.findById(normalizedGuruhId)
                    .orElseThrow(() -> new RuntimeException("Guruh topilmadi: " + normalizedGuruhId));

            user.setBosqichId(bosqichDto);
            user.setGuruhId(guruh);
            user.setKafedraId(null);
            user.setFanId(null);
            return;
        }

        if (role == Role.TEACHER ) {
            String normalizedKafedraId = requireNonBlank(kafedraId, "TEACHER uchun kafedra majburiy");
            String normalizedFanId = requireNonBlank(fanId, "TEACHER uchun fan majburiy");
            if (!kafedraRepository.existsById(normalizedKafedraId)) {
                throw new RuntimeException("Kafedra topilmadi: " + normalizedKafedraId);
            }
            if (!fanRepository.existsById(normalizedFanId)) {
                throw new RuntimeException("Fan topilmadi: " + normalizedFanId);
            }

            Kafedra kafedra = kafedraRepository.findById(normalizedKafedraId)
                    .orElseThrow(() -> new RuntimeException("Kafedra topilmadi: " + normalizedKafedraId));
            CatalogItemResponseDto kafedraDto = CatalogItemResponseDto.builder()
                    .id(kafedra.getId())
                    .name(kafedra.getName())
                    .build();

            Fan fan = fanRepository.findById(normalizedFanId)
                    .orElseThrow(() -> new RuntimeException("Fan topilmadi: " + normalizedFanId));



            user.setKafedraId(kafedraDto);
            user.setFanId(fan);
            user.setBosqichId(null);
            user.setGuruhId(null);
            return;
        }

        user.setBosqichId(null);
        user.setGuruhId(null);
        user.setKafedraId(null);
        user.setFanId(null);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }

    private UserResponseDto toResponse(User user) {
        UserResponseDto dto = UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .patronymic(user.getPatronymic())
                .profileImageUrl(user.getProfileImageUrl())
                .bosqich(user.getBosqichId())
                .guruh(user.getGuruhId())
                .kafedra(user.getKafedraId())
                .fan(user.getFanId())
                .build();
        return dto;
    }
}