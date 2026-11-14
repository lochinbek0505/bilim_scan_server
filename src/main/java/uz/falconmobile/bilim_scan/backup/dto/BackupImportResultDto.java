package uz.falconmobile.bilim_scan.backup.dto;

import java.util.Map;

public record BackupImportResultDto(
        Map<String, Integer> collections, // kolleksiya nomi -> tiklangan hujjatlar soni
        int files,                        // tiklangan rasmlar soni
        String previousStateBackup        // import oldidan saqlangan zaxira fayl yo'li
) {
}
