# NodeSplitter
Plugin for neo4j for splitting a single node into multiple nodes

Usage: `splitter.splitNodes([node1, node2]], {startIndex:0, indexProperty: 'Index', relationshipTypes: ['Type1', 'Type2']})`

### Arguments
1. List of nodes that to split
2. Parameters

### Parameters
|Parameter        |Description                                                                |Required                                           |
|-----------------|---------------------------------------------------------------------------|---------------------------------------------------|
|indexProperty    |Name of property that will be added to all result nodes and used as indexer|No                                                 |
|startIndex       |First index                                                                |Yes, will not be used if `indexProperty` is not set|
|relationshipTypes|List of relationship types that will be used for splitting                 |Yes                                                |
