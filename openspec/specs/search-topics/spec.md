## ADDED Requirements

### Requirement: search-topics-by-name tool
The `search-topics-by-name` tool SHALL search topics by name using client-side regex or glob matching against the Admin Client topic list.

#### Scenario: Search with prefix
- **WHEN** the tool is called with `searchTerm=my-app`
- **THEN** all topics containing "my-app" in their name are returned as a CSV string

#### Scenario: No matches
- **WHEN** the tool is called with a search term matching no topics
- **THEN** an empty string is returned
