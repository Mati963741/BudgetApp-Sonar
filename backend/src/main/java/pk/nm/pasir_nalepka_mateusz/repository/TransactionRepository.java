package pk.nm.pasir_nalepka_mateusz.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pk.nm.pasir_nalepka_mateusz.model.Transaction;
import pk.nm.pasir_nalepka_mateusz.model.User;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByUser(User user);
    List<Transaction> findByUser(User user);
    List<Transaction> findAllByUserAndTimestampGreaterThanEqual(User user, java.time.LocalDateTime timestamp);
}