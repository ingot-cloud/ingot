package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import com.ingot.cloud.iam.assignment.TenantLiveAssignmentService;
import com.ingot.cloud.iam.evaluation.*;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.*;
import com.ingot.cloud.iam.persistence.entity.IamDepartmentEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证统一浏览场景资格不混用，根投影只基于真实关系并遵循读取范围。</p>
 * @author jy
 * @since 1.0.0
 */
class OrganizationMemberPickerServiceTest {
    private final IamAccess access=mock(IamAccess.class);
    private final ResourceAccess scopes=mock(ResourceAccess.class);
    private final DepartmentNavigationService navigation=mock(DepartmentNavigationService.class);
    private final DepartmentQueryRepository departments=mock(DepartmentQueryRepository.class);
    private final OrganizationMemberRepository repository=mock(OrganizationMemberRepository.class);
    private final OrganizationMemberService members=mock(OrganizationMemberService.class);
    private final DepartmentLeaderService leaders=mock(DepartmentLeaderService.class);
    private final TenantLiveAssignmentService assignments=mock(TenantLiveAssignmentService.class);
    private final OrganizationMemberPickerService service=new OrganizationMemberPickerService(access,scopes,navigation,departments,repository,members,leaders,assignments);
    private final ActiveIdentity actor=new ActiveIdentity(new AuthorizationContext(AuthorizationDomain.TENANT,"10","1","101"),"0","0","0");
    private final ObjectScope readableScope=ObjectScope.all();
    private static final DepartmentNavigationEntry ROOT=new DepartmentNavigationEntry("1","组织");

    @BeforeEach
    void setup() {
        when(access.requireCurrent()).thenReturn(actor);
        when(access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_DEPARTMENT_READ,false)).thenReturn(true);
        when(access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_MEMBER_READ,false)).thenReturn(true);
        when(scopes.memberRead(actor.context(),IamAction.TENANT_MEMBER_READ)).thenReturn(readableScope);
        when(navigation.page(null,1,20)).thenReturn(nav(ROOT));
        when(members.pickerPage(any(),anyInt(),anyInt(),any())).thenReturn(new PageResponse<>(List.of(),0,1,20));
    }
    @Test
    void initialRootAndSearchUseDifferentBranchesWithSameScene() {
        browse(OrganizationMemberPickerScene.MEMBER,null,null,null,null);
        verify(members).pickerPage(null,1,20,MemberPickerBranch.department(1,true));
        browse(OrganizationMemberPickerScene.MEMBER,null,"张",null,null);
        verify(members).pickerPage("张",1,20,MemberPickerBranch.all());
        verifyNoInteractions(leaders,assignments);
    }
    @Test
    void leaderAndAssignmentCannotFallBackToGeneralMemberPool() {
        when(leaders.pickerCandidates(eq("11"),isNull(),eq(1),eq(20),any())).thenReturn(new DepartmentLeaderPage(List.of(),0,1,20,"0",false,null));
        when(assignments.pickerOptions(eq("61"),isNull(),eq(1),eq(20),any())).thenReturn(new PageResponse<>(List.of(),0,1,20));
        browse(OrganizationMemberPickerScene.DEPARTMENT_LEADER,null,null,"11",null);
        browse(OrganizationMemberPickerScene.ASSIGNMENT,null,null,null,"61");
        verify(leaders).pickerCandidates("11",null,1,20,MemberPickerBranch.department(1,true));
        verify(assignments).pickerOptions("61",null,1,20,MemberPickerBranch.department(1,true));
        verifyNoInteractions(members);
        when(assignments.pickerOptions(eq("62"),any(),anyInt(),anyInt(),any())).thenThrow(new BizException(IamReasonCode.ACTION_DENIED));
        assertThrows(BizException.class,()->browse(OrganizationMemberPickerScene.ASSIGNMENT,null,null,null,"62"));
    }
    @Test
    void invalidCrossSceneArgumentsAreRejectedBeforeAnyRead() {
        assertThrows(BizException.class,()->browse(OrganizationMemberPickerScene.MEMBER,null,null,"11",null));
        assertThrows(BizException.class,()->browse(OrganizationMemberPickerScene.MEMBER,null,null,null,"61"));
        assertThrows(BizException.class,()->browse(OrganizationMemberPickerScene.DEPARTMENT_LEADER,null,null,null,null));
        verifyNoInteractions(navigation,members,leaders,assignments);
    }
    @Test
    void noDepartmentReadRetainsOriginalPoolWithoutNamesAndRejectsExplicitBranch() {
        when(access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_DEPARTMENT_READ,false)).thenReturn(false);
        var result=browse(OrganizationMemberPickerScene.MEMBER,null,null,null,null);
        assertFalse(result.navigationAvailable());
        assertNull(result.navigation().organization());
        verify(members).pickerPage(null,1,20,MemberPickerBranch.all());
        verifyNoInteractions(navigation,departments,repository);
        assertThrows(BizException.class,()->browse(OrganizationMemberPickerScene.MEMBER,"11",null,null,null));
    }
    @Test
    void hiddenRootStartsWithFragmentsButSearchStillWorks() {
        when(navigation.page(null,1,20)).thenReturn(nav(null));
        browse(OrganizationMemberPickerScene.MEMBER,null,null,null,null);
        verify(members).pickerPage(null,1,20,MemberPickerBranch.none());
        browse(OrganizationMemberPickerScene.MEMBER,null,"张",null,null);
        verify(members).pickerPage("张",1,20,MemberPickerBranch.all());
        verifyNoInteractions(repository);
    }
    @Test
    void explicitBranchesRequireVisibilityEvenWhenSearching() {
        var row=new IamDepartmentEntity();row.setParentId(BigInteger.ONE);
        when(departments.find(10,11)).thenReturn(row);
        when(navigation.page("11",1,20)).thenReturn(nav(ROOT));
        browse(OrganizationMemberPickerScene.MEMBER,"11",null,null,null);
        verify(members).pickerPage(null,1,20,MemberPickerBranch.department(11,false));
        doThrow(new BizException(IamReasonCode.ACTION_DENIED)).when(scopes).requireVisibleDepartment(actor.context(),IamAction.TENANT_DEPARTMENT_READ,12);
        when(navigation.page("12",1,20)).thenReturn(nav(ROOT));
        assertThrows(BizException.class,()->browse(OrganizationMemberPickerScene.MEMBER,"12","张",null,null));
    }
    @Test
    void implicitRootIsSeparateFromActualRelationshipsAndDoesNotInferFromEmptyProjection() {
        when(members.pickerPage(any(),anyInt(),anyInt(),any())).thenReturn(new PageResponse<>(List.of(
                new MemberRecord("104","零任职",null,null,null,null,MemberStatus.ACTIVE,List.of()),
                new MemberRecord("108","隐藏任职",null,null,null,null,MemberStatus.ACTIVE,List.of())),2,1,20));
        when(repository.unassigned(10,List.of(BigInteger.valueOf(104),BigInteger.valueOf(108)),readableScope)).thenReturn(Set.of(BigInteger.valueOf(104)));
        var result=browse(OrganizationMemberPickerScene.MEMBER,null,null,null,null);
        assertEquals(ROOT,result.members().items().getFirst().implicitRoot());
        assertTrue(result.members().items().getFirst().departments().isEmpty());
        assertNull(result.members().items().getLast().implicitRoot());
    }
    @Test
    void assignmentWithoutMemberReadNeverEnrichesRootLabel() {
        when(access.allows(AuthorizationDomain.TENANT,IamAction.TENANT_MEMBER_READ,false)).thenReturn(false);
        when(assignments.pickerOptions(any(),any(),anyInt(),anyInt(),any())).thenReturn(new PageResponse<>(List.of(
                new TenantAssignmentOption("104","接收人",TenantAssignmentOptionKind.MEMBER,null)),1,1,20));
        assertNull(browse(OrganizationMemberPickerScene.ASSIGNMENT,null,null,null,null).members().items().getFirst().implicitRoot());
        verifyNoInteractions(repository);
    }
    private OrganizationMemberPickerPage browse(OrganizationMemberPickerScene scene,String department,String keyword,String target,String source) {
        return service.browse(scene,department,keyword,target,source,1,20,1,20);
    }
    private static DepartmentNavigationPage nav(DepartmentNavigationEntry root) {
        return new DepartmentNavigationPage(List.of(new DepartmentNavigationEntry("11","研发")),1,1,20,List.of(),root);
    }
}
