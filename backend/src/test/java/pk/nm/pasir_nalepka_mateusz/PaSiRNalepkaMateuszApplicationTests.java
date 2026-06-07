package pk.nm.pasir_nalepka_mateusz;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import pk.nm.pasir_nalepka_mateusz.model.Group;
import pk.nm.pasir_nalepka_mateusz.model.User;
import pk.nm.pasir_nalepka_mateusz.repository.GroupRepository;
import pk.nm.pasir_nalepka_mateusz.repository.UserRepository;
import pk.nm.pasir_nalepka_mateusz.service.DebtService;
import pk.nm.pasir_nalepka_mateusz.service.GroupService;
import pk.nm.pasir_nalepka_mateusz.service.MembershipService;
import pk.nm.pasir_nalepka_mateusz.service.GroupTransactionService;

import pk.nm.pasir_nalepka_mateusz.dto.GroupDTO;
import pk.nm.pasir_nalepka_mateusz.dto.MembershipDTO;
import pk.nm.pasir_nalepka_mateusz.dto.DebtDTO;
import pk.nm.pasir_nalepka_mateusz.dto.GroupTransactionDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PaSiRNalepkaMateuszApplicationTests {

	@Autowired
	private GroupService groupService;
	@Autowired
	private MembershipService membershipService;
	@Autowired
	private DebtService debtService;
	@Autowired
	private GroupTransactionService groupTransactionService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private GroupRepository groupRepository;

	private User createUser(String email) {
		User user = new User();
		user.setEmail(email);
		user.setPassword("password");
		user.setUsername(email.split("@")[0]);
		return userRepository.save(user);
	}

	private Group setupGroupWithOwnerAndMember(User member) {
		GroupDTO dto = new GroupDTO();
		dto.setName("Wspólna Grupa");
		Group group = groupService.createGroup(dto);

		MembershipDTO mDto = new MembershipDTO();
		mDto.setUserEmail(member.getEmail());
		mDto.setGroupId(group.getId());
		membershipService.addMember(mDto);

		return group;
	}

	@Test
	@WithMockUser(username = "owner@example.com")
	public void createGroup_AddsOwnerAsMember_AndReturnsInMyGroups() {
		createUser("owner@example.com");

		GroupDTO dto = new GroupDTO();
		dto.setName("Wyjazd");
		Group group = groupService.createGroup(dto);

		var myGroups = groupService.getAllGroups();
		assertTrue(myGroups.stream().anyMatch(g -> g.getId().equals(group.getId())));
	}

	@Test
	@WithMockUser(username = "member@example.com")
	public void addMember_ByNonOwner_ThrowsAccessDenied() {
		User owner = createUser("owner@example.com");
		createUser("member@example.com");
		User newGuy = createUser("newguy@example.com");

		Group group = new Group();
		group.setName("Grupa");
		group.setOwner(owner);
		group = groupRepository.save(group);

		MembershipDTO mDto = new MembershipDTO();
		mDto.setUserEmail(newGuy.getEmail());
		mDto.setGroupId(group.getId());

		assertThrows(AccessDeniedException.class, () -> {
			membershipService.addMember(mDto);
		});
	}

	@Test
	@WithMockUser(username = "stranger@example.com")
	public void getGroupMembers_ByNonMember_ThrowsAccessDenied() {
		User owner = createUser("owner@example.com");
		createUser("stranger@example.com");

		Group group = new Group();
		group.setName("Grupa");
		group.setOwner(owner);
		group = groupRepository.save(group);

		Group finalGroup = group;
		assertThrows(AccessDeniedException.class, () -> {
			membershipService.getGroupMembers(finalGroup.getId());
		});
	}

	@Test
	@WithMockUser(username = "stranger@example.com")
	public void getGroupDebts_ByNonMember_ThrowsAccessDenied() {
		User owner = createUser("owner@example.com");
		createUser("stranger@example.com");

		Group group = new Group();
		group.setName("Grupa");
		group.setOwner(owner);
		group = groupRepository.save(group);

		Group finalGroup = group;
		assertThrows(AccessDeniedException.class, () -> {
			debtService.getGroupDebts(finalGroup.getId());
		});
	}

	@Test
	@WithMockUser(username = "owner@example.com")
	public void addGroupTransaction_Income_CreatesDebts() {
		User owner = createUser("owner@example.com");
		User member = createUser("member@example.com");
		Group group = setupGroupWithOwnerAndMember(member);

		GroupTransactionDTO tDto = new GroupTransactionDTO();
		tDto.setGroupId(group.getId());
		tDto.setAmount(100.0);
		tDto.setType("INCOME");
		tDto.setTitle("Test");
		tDto.setSelectedUserIds(List.of(owner.getId(), member.getId()));

		groupTransactionService.addGroupTransaction(tDto);

		var debts = debtService.getGroupDebts(group.getId());
		assertFalse(debts.isEmpty(), "Długi powinny zostać wygenerowane");
	}

	@Test
	@WithMockUser(username = "owner@example.com")
	public void removeMember_KeepsHistoricalDebts() {
		User owner = createUser("owner@example.com");
		User member = createUser("member@example.com");
		Group group = setupGroupWithOwnerAndMember(member);

		DebtDTO dDto = new DebtDTO();
		dDto.setDebtorId(member.getId());
		dDto.setCreditorId(owner.getId());
		dDto.setGroupId(group.getId());
		dDto.setAmount(50.0);
		dDto.setTitle("Test długu");
		debtService.createDebt(dDto);

		Long memberMembershipId = membershipService.getGroupMembers(group.getId()).stream()
				.filter(m -> m.getUser().getId().equals(member.getId()))
				.findFirst().get().getId();

		membershipService.removeMember(memberMembershipId);

		var debts = debtService.getGroupDebts(group.getId());
		assertEquals(1, debts.size(), "Dług historyczny musi pozostać");
	}

	@Test
	@WithMockUser(username = "owner@example.com")
	public void removeMember_Owner_ThrowsException() {
		createUser("owner@example.com");
		GroupDTO dto = new GroupDTO();
		dto.setName("Góry");
		Group group = groupService.createGroup(dto);

		Long ownerMembershipId = membershipService.getGroupMembers(group.getId()).get(0).getId();

		assertThrows(IllegalStateException.class, () -> {
			membershipService.removeMember(ownerMembershipId);
		});
	}

	@Test
	@WithMockUser(username = "member@example.com")
	public void deleteGroup_ByNonOwner_ThrowsAccessDenied() {
		User owner = createUser("owner@example.com");
		createUser("member@example.com");

		Group group = new Group();
		group.setName("Wspólna Grupa");
		group.setOwner(owner);
		group = groupRepository.save(group);

		final Long groupId = group.getId();
		assertThrows(AccessDeniedException.class, () -> {
			groupService.deleteGroup(groupId);
		});
	}

	@Test
	@WithMockUser(username = "owner@example.com")
	public void createDebt_SelfDebtOrOutsideGroup_ThrowsException() {
		User owner = createUser("owner@example.com");
		User stranger = createUser("stranger@example.com");

		GroupDTO dto = new GroupDTO();
		dto.setName("Góry");
		Group group = groupService.createGroup(dto);

		DebtDTO selfDebtDto = new DebtDTO();
		selfDebtDto.setDebtorId(owner.getId());
		selfDebtDto.setCreditorId(owner.getId());
		selfDebtDto.setGroupId(group.getId());
		selfDebtDto.setAmount(50.0);
		selfDebtDto.setTitle("Sam do siebie");

		assertThrows(RuntimeException.class, () -> {
			debtService.createDebt(selfDebtDto);
		});

		DebtDTO outsideDebtDto = new DebtDTO();
		outsideDebtDto.setDebtorId(owner.getId());
		outsideDebtDto.setCreditorId(stranger.getId());
		outsideDebtDto.setGroupId(group.getId());
		outsideDebtDto.setAmount(50.0);
		outsideDebtDto.setTitle("Do obcego");

		assertThrows(RuntimeException.class, () -> {
			debtService.createDebt(outsideDebtDto);
		});
	}

	@Test
	@WithMockUser(username = "owner@example.com")
	public void createDebt_ByOwnerForOthers_Succeeds() {
		createUser("owner@example.com");
		User member1 = createUser("member1@example.com");
		User member2 = createUser("member2@example.com");

		Group group = setupGroupWithOwnerAndMember(member1);

		MembershipDTO mDto2 = new MembershipDTO();
		mDto2.setUserEmail(member2.getEmail());
		mDto2.setGroupId(group.getId());
		membershipService.addMember(mDto2);

		DebtDTO debtDto = new DebtDTO();
		debtDto.setDebtorId(member1.getId());
		debtDto.setCreditorId(member2.getId());
		debtDto.setGroupId(group.getId());
		debtDto.setAmount(50.0);
		debtDto.setTitle("Test");

		assertDoesNotThrow(() -> {
			debtService.createDebt(debtDto);
		});
	}

	@Test
	@WithMockUser(username = "owner@example.com")
	public void deleteDebt_ByOwner_Succeeds() {
		createUser("owner@example.com");
		User member1 = createUser("member1@example.com");
		User member2 = createUser("member2@example.com");

		Group group = setupGroupWithOwnerAndMember(member1);

		MembershipDTO mDto = new MembershipDTO();
		mDto.setUserEmail(member2.getEmail());
		mDto.setGroupId(group.getId());
		membershipService.addMember(mDto);

		DebtDTO debtDto = new DebtDTO();
		debtDto.setDebtorId(member1.getId());
		debtDto.setCreditorId(member2.getId());
		debtDto.setGroupId(group.getId());
		debtDto.setAmount(50.0);
		debtDto.setTitle("Test");

		var debt = debtService.createDebt(debtDto);

		assertDoesNotThrow(() -> {
			debtService.deleteDebt(debt.getId());
		});
	}

	@Test
	@WithMockUser(username = "owner@example.com")
	public void createGroup_WithEmptyName_ThrowsException() {
		createUser("owner@example.com");

		GroupDTO dto = new GroupDTO();
		dto.setName("");

		assertThrows(Exception.class, () -> {
			groupService.createGroup(dto);
		});
	}

	@Test
	@WithMockUser(username = "owner@example.com")
	public void deleteGroup_ByOwner_RemovesEverything() {
		createUser("owner@example.com");

		GroupDTO dto = new GroupDTO();
		dto.setName("Usuwana");
		Group group = groupService.createGroup(dto);

		groupService.deleteGroup(group.getId());

		assertTrue(groupRepository.findById(group.getId()).isEmpty(), "Grupa powinna zostać usunięta z bazy");
	}

}