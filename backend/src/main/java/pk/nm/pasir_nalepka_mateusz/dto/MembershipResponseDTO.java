package pk.nm.pasir_nalepka_mateusz.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MembershipResponseDTO {
    private Long id;
    private Long userId;
    private Long groupId;
    private String userEmail;
}
