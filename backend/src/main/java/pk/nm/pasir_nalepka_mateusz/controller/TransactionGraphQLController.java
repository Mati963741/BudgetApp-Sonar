package pk.nm.pasir_nalepka_mateusz.controller;

import pk.nm.pasir_nalepka_mateusz.dto.BalanceDTO;
import pk.nm.pasir_nalepka_mateusz.model.Transaction;
import pk.nm.pasir_nalepka_mateusz.model.User;
import pk.nm.pasir_nalepka_mateusz.service.TransactionService;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import jakarta.validation.Valid;
import pk.nm.pasir_nalepka_mateusz.dto.TransactionDTO;

@Controller
public class TransactionGraphQLController {

    private final TransactionService transactionService;

    public TransactionGraphQLController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @QueryMapping
    public List<Transaction> transactions() {
        return transactionService.getAllTransactions();
    }

    @MutationMapping
    public Transaction addTransaction(@Valid @Argument TransactionDTO transactionDTO) {
        return transactionService.saveTransaction(transactionDTO);
    }

    @MutationMapping
    public Transaction updateTransaction(@Argument Long id, @Valid @Argument TransactionDTO transactionDTO) {
        return transactionService.updateTransaction(id, transactionDTO);
    }

    @MutationMapping
    public Boolean deleteTransaction(@Argument Long id) {
        try {
            transactionService.deleteTransaction(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @QueryMapping
    public BalanceDTO userBalance(@Argument Double days) {

        User user = transactionService.getCurrentUser();

        return transactionService.getUserBalance(user, days);
    }
}