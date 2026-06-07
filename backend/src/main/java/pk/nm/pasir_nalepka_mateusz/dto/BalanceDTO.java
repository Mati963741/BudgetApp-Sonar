package pk.nm.pasir_nalepka_mateusz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BalanceDTO {
    private double totalIncome;
    private double totalExpense;
    private double balance;
}