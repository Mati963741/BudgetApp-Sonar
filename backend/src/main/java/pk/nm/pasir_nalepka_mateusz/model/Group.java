package pk.nm.pasir_nalepka_mateusz.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "`groups`")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Membership> memberships;

    @SuppressWarnings("unused")
    @Transient
    public Long getOwnerId() {
        return owner != null ? owner.getId() : null;
    }
}
