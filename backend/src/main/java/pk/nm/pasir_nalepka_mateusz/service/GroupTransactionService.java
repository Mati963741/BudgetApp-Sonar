package pk.nm.pasir_nalepka_mateusz.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import pk.nm.pasir_nalepka_mateusz.config.NotificationWebSocketHandler;
import pk.nm.pasir_nalepka_mateusz.dto.GroupTransactionDTO;
import pk.nm.pasir_nalepka_mateusz.model.Debt;
import pk.nm.pasir_nalepka_mateusz.model.Group;
import pk.nm.pasir_nalepka_mateusz.model.Membership;
import pk.nm.pasir_nalepka_mateusz.model.User;
import pk.nm.pasir_nalepka_mateusz.repository.DebtRepository;
import pk.nm.pasir_nalepka_mateusz.repository.GroupRepository;
import pk.nm.pasir_nalepka_mateusz.repository.MembershipRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pk.nm.pasir_nalepka_mateusz.dto.GroupNotificationDTO;
import pk.nm.pasir_nalepka_mateusz.repository.TransactionRepository;


@Service
public class GroupTransactionService {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final MembershipService membershipService;
    private final CurrentUserService currentUserService;
    private final NotificationWebSocketHandler notificationHandler;
    private final TransactionRepository transactionRepository;

    public GroupTransactionService(
            GroupRepository groupRepository,
            MembershipRepository membershipRepository,
            DebtRepository debtRepository,
            MembershipService membershipService,
            CurrentUserService currentUserService,
            NotificationWebSocketHandler notificationHandler,
            TransactionRepository transactionRepository) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.debtRepository = debtRepository;
        this.membershipService = membershipService;
        this.currentUserService = currentUserService;
        this.notificationHandler = notificationHandler;
        this.transactionRepository = transactionRepository;
    }

    public void addGroupTransaction(GroupTransactionDTO transactionDTO) {
        User currentUser = currentUserService.getCurrentUser();

        Group group = groupRepository.findById(transactionDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono Grupy"));

        membershipService.assertCurrentUserIsGroupMember(group.getId());

        List<Membership> members = membershipRepository.findByGroupId(group.getId());
        List<Membership> selectedMembers = selectParticipants(transactionDTO, members, currentUser);

        if (selectedMembers.isEmpty()) {
            throw new IllegalStateException("Grupa nie ma członków, nie można dodać transakcji.");
        }

        double amountPerUser = transactionDTO.getAmount() / selectedMembers.size();
        boolean expense = "EXPENSE".equals(transactionDTO.getType());

        pk.nm.pasir_nalepka_mateusz.model.Transaction personalExpense = new pk.nm.pasir_nalepka_mateusz.model.Transaction();
        personalExpense.setUser(currentUser);
        personalExpense.setAmount(transactionDTO.getAmount());

        personalExpense.setType(pk.nm.pasir_nalepka_mateusz.model.TransactionType.valueOf(transactionDTO.getType()));

        personalExpense.setNotes("Wspólny wydatek (" + group.getName() + "): " + transactionDTO.getTitle());

        personalExpense.setTimestamp(java.time.LocalDateTime.now());

        transactionRepository.save(personalExpense);

        for (Membership member : selectedMembers) {
            User otherUser = member.getUser();
            if (!otherUser.getId().equals(currentUser.getId())) {
                Debt debt = new Debt();
                debt.setDebtor(expense ? otherUser : currentUser);
                debt.setCreditor(expense ? currentUser : otherUser);
                debt.setGroup(group);
                debt.setAmount(amountPerUser);
                debt.setTitle(transactionDTO.getTitle());
                debtRepository.save(debt);

                String messageText = String.format(java.util.Locale.US,
                        "%s dodał wydatek \"%s\" w grupie %s. Twoja część: %.2f zł.",
                        currentUser.getEmail(), transactionDTO.getTitle(), group.getName(), amountPerUser);

                GroupNotificationDTO notification = new GroupNotificationDTO(
                        "GROUP_EXPENSE_ADDED",
                        group.getId(),
                        group.getName(),
                        transactionDTO.getTitle(),
                        transactionDTO.getAmount(),
                        amountPerUser,
                        currentUser.getEmail(),
                        messageText
                );

                notificationHandler.sendNotificationToUser(otherUser.getEmail(), notification);
            }
        }
    }

    private List<Membership> selectParticipants(
            GroupTransactionDTO transactionDTO,
            List<Membership> members,
            User currentUser) {

        List<Long> selectedUserIds = transactionDTO.getSelectedUserIds();

        if (selectedUserIds == null || selectedUserIds.isEmpty()) {
            return members;
        }

        Set<Long> uniqueSelectedUserIds = new HashSet<>(selectedUserIds);

        List<Membership> selectedMembers = members.stream()
                .filter(membership -> uniqueSelectedUserIds.contains(membership.getUser().getId()))
                .toList();

        if (selectedMembers.size() != uniqueSelectedUserIds.size()) {
            throw new IllegalStateException("Wszyscy wybrani uzytkownicy musza byc członkami grupy.");
        }

        boolean currentUserSelected = selectedMembers.stream()
                .anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));

        if (!currentUserSelected) {
            throw new IllegalStateException("Aktualny uzytkownik musi byc uczestnikiem transakcji grupowej.");
        }

        if (selectedMembers.size() < 2) {
            throw new IllegalStateException("Transakcja grupowa wymaga co najmniej dwoch uczestnikow.");
        }

        return selectedMembers;
    }
}
