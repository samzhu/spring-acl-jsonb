-- Test data for ACL demo project
-- Users: alice, bob, charlie
-- Groups: admin, developer
-- Permissions: read, write, delete

-- Clean up existing data
DELETE FROM project;

-- Project 1: Alice has full permissions (read, write, delete)
INSERT INTO project (id, name, description, acl_entries, created_time, modified_time, version)
VALUES (1, 'Alice Full Access Project',
        'Project where Alice has full permissions',
        '["user:alice:read", "user:alice:write", "user:alice:delete"]'::jsonb,
        '2024-01-01T10:00:00Z', '2024-01-01T10:00:00Z', 0);

-- Project 2: Bob has read permission only
INSERT INTO project (id, name, description, acl_entries, created_time, modified_time, version)
VALUES (2, 'Bob Read-Only Project',
        'Project where Bob can only read',
        '["user:bob:read", "user:alice:read", "user:alice:write", "user:alice:delete"]'::jsonb,
        '2024-01-02T10:00:00Z', '2024-01-02T10:00:00Z', 0);

-- Project 3: Admin group has full permissions
INSERT INTO project (id, name, description, acl_entries, created_time, modified_time, version)
VALUES (3, 'Admin Group Project',
        'Project where admin group has full access',
        '["group:admin:read", "group:admin:write", "group:admin:delete"]'::jsonb,
        '2024-01-03T10:00:00Z', '2024-01-03T10:00:00Z', 0);

-- Project 4: Developer group has read/write, no delete
INSERT INTO project (id, name, description, acl_entries, created_time, modified_time, version)
VALUES (4, 'Developer Group Project',
        'Project where developer group can read and write',
        '["group:developer:read", "group:developer:write"]'::jsonb,
        '2024-01-04T10:00:00Z', '2024-01-04T10:00:00Z', 0);

-- Project 5: Mixed permissions - Alice user + developer group
INSERT INTO project (id, name, description, acl_entries, created_time, modified_time, version)
VALUES (5, 'Mixed Permissions Project',
        'Project with both user and group permissions',
        '["user:alice:read", "group:developer:write", "group:admin:delete"]'::jsonb,
        '2024-01-05T10:00:00Z', '2024-01-05T10:00:00Z', 0);

-- Project 6: Charlie has no permissions (for negative testing)
INSERT INTO project (id, name, description, acl_entries, created_time, modified_time, version)
VALUES (6, 'Private Project',
        'Project where Charlie has no access',
        '["user:alice:read", "user:alice:write", "user:alice:delete"]'::jsonb,
        '2024-01-06T10:00:00Z', '2024-01-06T10:00:00Z', 0);

-- Project 7: Public read for testing search with multiple results
INSERT INTO project (id, name, description, acl_entries, created_time, modified_time, version)
VALUES (7, 'Public Read Project Alpha',
        'Project searchable by keyword Alpha',
        '["user:alice:read", "user:bob:read", "user:charlie:read"]'::jsonb,
        '2024-01-07T10:00:00Z', '2024-01-07T10:00:00Z', 0);

-- Project 8: Another public read for testing pagination
INSERT INTO project (id, name, description, acl_entries, created_time, modified_time, version)
VALUES (8, 'Public Read Project Beta',
        'Project searchable by keyword Beta',
        '["user:alice:read", "user:bob:read", "user:charlie:read"]'::jsonb,
        '2024-01-08T10:00:00Z', '2024-01-08T10:00:00Z', 0);

-- Update sequence to start from 9 (after test data 1-8)
SELECT setval('project_id_seq', 8);
