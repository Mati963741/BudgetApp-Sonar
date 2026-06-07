package pk.nm.pasir_nalepka_mateusz.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pk.nm.pasir_nalepka_mateusz.model.Group;
import pk.nm.pasir_nalepka_mateusz.model.User;
import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByMemberships_User(User user);
}
