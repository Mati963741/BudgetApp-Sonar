package pk.nm.pasir_nalepka_mateusz.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import pk.nm.pasir_nalepka_mateusz.dto.GroupTransactionDTO;
import pk.nm.pasir_nalepka_mateusz.service.GroupTransactionService;

@Controller
public class GroupTransactionGraphQLController {

    private final GroupTransactionService groupTransactionService;

    public GroupTransactionGraphQLController(
            GroupTransactionService groupTransactionService) {
        this.groupTransactionService = groupTransactionService;
    }

    @MutationMapping
    public Boolean addGroupTransaction(@Valid @Argument GroupTransactionDTO groupTransactionDTO) {
        groupTransactionService.addGroupTransaction(groupTransactionDTO);
        return true;
    }
}
