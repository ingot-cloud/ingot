import copy
import unittest
from migrate import preflight,forward,reverse,digest,normalize_ceiling,instant


def fixture():
    return {
      'iam_tenant':[{'id':10,'owner_member_id':101}],
      'iam_account':[{'id':1,'enabled':True},{'id':2,'enabled':True}],
      'iam_tenant_member':[{'id':101,'account_id':1,'tenant_id':10,'status':'ACTIVE'},{'id':102,'account_id':2,'tenant_id':10,'status':'ACTIVE'}],
      'iam_application':[{'id':1,'domain':'TENANT'}],
      'iam_resource':[{'id':6,'application_id':1}],
      'iam_action':[{'id':11,'application_id':1,'resource_id':6},{'id':12,'application_id':1,'resource_id':6}],
      'iam_role_definition':[{'id':1,'domain':'TENANT','kind':'SHARED'},{'id':2,'domain':'TENANT','kind':'TENANT_CUSTOM','tenant_id':10},{'id':99,'domain':'TENANT','kind':'SYSTEM'}],
      'iam_role_revision':[{'id':1,'role_id':1,'revision':1},{'id':3,'role_id':1,'revision':2},{'id':4,'role_id':2,'revision':1,'base_revision_id':1},{'id':99,'role_id':99,'revision':1}],
      'iam_role_grant':[{'revision_id':1,'action_id':11,'scopes':[{'kind':'ALL'}]},{'revision_id':3,'action_id':12,'scopes':[{'kind':'ALL'}]}],
      'iam_role_delta':[{'revision_id':4,'action_id':11,'operation':'REPLACE_SCOPE','scopes':[{'kind':'SELF'}]}],
      'iam_role_parameter':[],
      'iam_role_assignment':[{'id':50,'domain':'TENANT','tenant_id':10,'subject_type':'MEMBER','tenant_member_id':102,'revision_id':1,'status':'ACTIVE','source':'MANUAL','scope_bindings':{}}, {'id':51,'domain':'TENANT','tenant_id':10,'subject_type':'MEMBER','tenant_member_id':101,'revision_id':99,'status':'ACTIVE','source':'INITIALIZATION','scope_bindings':{}}],
      'iam_delegation_grant':[],
    }


class MigrationTest(unittest.TestCase):
    def test_private_materializes_its_historical_base_and_shared_uses_latest(self):
        report=preflight(fixture(),'fixture')
        self.assertTrue(report['ready'],report['conflicts'])
        shared,private=report['roles']
        self.assertEqual('12',shared['grants'][0]['actionId'])
        self.assertEqual([{'actionId':'11','scopes':[{'kind':'SELF'}]}],private['grants'])
        self.assertEqual([12],report['assignments'][0]['addedActionIds'])
        self.assertTrue(report['assignments'][1]['retiredOwner'])
        sql=forward(report,fixture())
        self.assertIn("LEAVE main",sql)
        self.assertIn('iam_live_authorization_mapping',sql)
        self.assertEqual(sql,forward(preflight(fixture(),'fixture'),fixture()))
    def test_source_changes_reject_old_report_and_resolution(self):
        source=fixture();report=preflight(source,'fixture');source['iam_tenant'][0]['owner_member_id']=102
        with self.assertRaises(ValueError):forward(report,source)
        with self.assertRaises(ValueError):preflight(source,'fixture',{'sourceDigest':report['sourceDigest'],'decisions':[]})
    def test_nonowner_system_blocks_until_explicit_revoke(self):
        source=fixture();source['iam_role_assignment'][1]['tenant_member_id']=102
        report=preflight(source,'fixture');self.assertFalse(report['ready'])
        resolution={'sourceDigest':digest(source),'decisions':[{'conflictId':'SYSTEM_ASSIGNMENT:51','actor':'fixture-operator','reason':'取消历史特权','at':'2026-10-14T01:00:00Z','value':{'revoke':True}}]}
        report=preflight(source,'fixture',resolution);self.assertTrue(report['ready'],report['conflicts']);self.assertEqual('REVOKED',report['assignments'][1]['status'])
    def test_different_action_ceilings_never_union_automatically(self):
        source=fixture();source['iam_delegation_grant']=[{'id':60,'domain':'TENANT','tenant_id':10}]
        source['iam_delegation_role_revision']=[{'delegation_id':60,'revision_id':1}]
        source['iam_delegation_action_ceiling']=[{'delegation_id':60,'action_id':11,'scopes':[{'kind':'ALL'}],'scope_bindings':{}},{'delegation_id':60,'action_id':12,'scopes':[{'kind':'SELF'}],'scope_bindings':{}}]
        report=preflight(source,'fixture');self.assertFalse(report['ready']);self.assertEqual('RESOURCE_CEILING:60:6',report['conflicts'][0]['conflictId'])
        with self.assertRaises(ValueError):forward(report)
    def test_ceiling_normalization_preserves_semantics_not_json_order(self):
        self.assertEqual(normalize_ceiling([{'kind':'MANAGED_DEPARTMENTS','parameterKey':'dept'}],{'dept':{'kind':'DEPARTMENTS','ids':['2','1']}}),normalize_ceiling([{'kind':'MANAGED_DEPARTMENTS','parameterKey':'dept','includeDescendants':False}],{'dept':{'kind':'DEPARTMENTS','ids':['1','2']}}))
    def test_sql_strings_cannot_escape_literals(self):
        source=fixture();source['iam_role_definition'][0]['name']="'); DROP TABLE iam_account; --"
        sql=forward(preflight(source,'safe'))
        self.assertNotIn('DROP TABLE iam_account',sql)
    def test_invalid_owner_cannot_be_hidden_with_resolution(self):
        source=fixture();source['iam_tenant'][0]['owner_member_id']=999
        report=preflight(source,'fixture',{'sourceDigest':digest(source),'decisions':[{'conflictId':'INVALID_OWNER:10','actor':'operator','reason':'不允许绕过','at':'2026-10-14T01:00:00Z','value':{}}]})
        self.assertFalse(report['ready'])

    def test_reusing_a_historical_parameter_with_a_different_type_blocks(self):
        source=fixture();source['iam_resource'][0]['scope_capabilities']=['ALL','SELF','OBJECT_SET']
        source['iam_role_grant'][1]['scopes']=[{'kind':'OBJECT_SET','parameterKey':'range'}]
        source['iam_role_parameter']=[{'revision_id':1,'parameter_key':'range','binding_kind':'DEPARTMENTS'},{'revision_id':3,'parameter_key':'range','binding_kind':'OBJECTS'}]
        report=preflight(source,'types');self.assertFalse(report['ready']);self.assertTrue(any(c['conflictId']=='INVALID_ROLE:1' for c in report['conflicts']))

    def test_negative_offset_keeps_the_same_instant_and_naive_resolution_is_rejected(self):
        self.assertEqual(instant('2026-10-14T01:00:00Z'),instant('2026-10-13T18:00:00-07:00'))
        source=fixture();source['iam_role_assignment'][1]['tenant_member_id']=102
        with self.assertRaises(ValueError):preflight(source,'fixture',{'sourceDigest':digest(source),'decisions':[{'conflictId':'SYSTEM_ASSIGNMENT:51','actor':'operator','reason':'撤销','at':'2026-10-14T01:00:00','value':{'revoke':True}}]})

if __name__=='__main__':unittest.main()
