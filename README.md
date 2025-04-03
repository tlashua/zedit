
#WIP
Next Steps
Now that we've completed this cleanup, we're ready for another significant refactoring that will make a real difference. Some potential areas for further refactoring could include:
State Management: Refactoring how state is managed in the ZoneCanvas component
UI Component Separation: Breaking down the ZoneCanvas into smaller, more focused components
Event Handling: Simplifying the event handling logic in ZoneCanvas



#readme
This is a zone editor for AberMUDs.  It's still in development and lacks several features
needed to make it 'better than a text editor.'

A zone consists of locations (rooms), mobiles, and objects.  The rooms form a directed graph.

Features that work or should work now
- add room with button
- add room by dragging from connection point on room
- add connection between rooms by dragging from connection point to the other room
- delete connection with right click context menu
- move connection to new room, or create new room from existing connection point
- save to file
- load from file
- change zone name
- re-number all rooms, creating sequential ids in the form <zone-name>room_number
- change room name
- add room description
  - very limited now
- snap to grid


Connections

These are edges in the directed graph, therefore a bi-directional connection requires two edges.
Bi-directional connections are displayed with a single line that has an arrowhead on each end, but
that still represents two edges.  Basic rules of connections:
- A room (node) can only have on exit in each of the six directions
  - north
  - south
  - east
  - west
  - up
  - down
- A room can have any number of incoming connections
- Connections can be one-way
- Connections can originate from a room, an object, magic, and possibly other sources.
  They are not restricted to coming from another room.
- Connections originating at a room can only go to another room.  It makes no sense for them to
  go to an object, or anything but a room.
- _connection points_ are dots on the border of a room used for display purposes.  Currently
  there is one for each direction n, s, e, w, and two for up and down.  There is no reason
  to limit the number of connection points, as their purpose is to fix the link arrow to a
  specific point on the room, for display purposes.
- While it is typical that an exit in one room leads to the opposite in another, it is not
  required.  It is perfectly correct for room a to have an east exit to room b, and room b to
  have an east exit back to room a.  It's a little odd, but people do it and sometimes it
  makes sense.  It is also common for an exit to lead back into the room it came from.  That
  is useful for mazes and various other things.

BUGS
You can only have one connection coming into the same side of a room.  There should be no limit.

TODO
- delete room
- multi-select
- area select
- choose connection point on destination
- room editor
  - attributes
  - flags
  - editable text pane
- mobiles
- objects
- mobile editor
  - description and examine text
  - flags
  - location
  - attributes
- object editor
    - description and examine text
    - flags
    - location
    - attributes
    - state
- export to AberMUD zone file format
- import from AberMUD zone file format


Principles of Refactoring
There are a few basics about refactoring that are crucial for success.
- refactoring is a disciplined method of changing code to make it easier to work with, while maintaining functionality
- when refactoring, you should not add features
- it must follow an iterative, incremental process
- if code must be broken, then it must only be broken the minimum amount of time to complete that specific step
- refactoring steps should minimize broken code


Unsorted Notes

x snap to grid
- align nodes horizontally
- align nodes vertically
- multi-select
- select by region
- attributes on rooms
- mobiles (NPCs)
- objects
- separation of the data model with layout.  as you can see in scratchpad.zone, screen layout is mixed with the zone data.  screen layout is useful for the editor and for generating maps, but useless in the game
- editor improvements - at min, make it 79 chars wide, which is the max width of text displayed in this terminal game

That's a thoughtful list that covers both UI/UX improvements and architectural concerns. Let me analyze it:
Critical features I'd keep:
- Snap to grid (helps create cleaner layouts)
- Multi-select and select by region (fundamental editor functionality)
- Separation of data model from layout (important architectural improvement)
- Room attributes (essential for gameplay)
- Features I'd add:
- Undo/redo functionality (crucial for any editor)
- Keyboard shortcuts (efficiency for power users)
- Room templates/presets (speed up zone creation)
- Basic validation (prevent invalid connections, duplicate IDs)

For the next feature, I'd prioritize snap to grid because:
- It's a relatively contained change (mostly in ZoneCanvas.kt)
- It will immediately improve the quality of created zones
- It's a prerequisite for clean alignment features
- It will help with the eventual data model separation by establishing standard positioning
- The implementation would involve:
- Adding a grid size setting
- Modifying the room dragging logic to snap positions
- Adding a toggle for snap behavior
- Updating the position calculations in new room creation
- Would you like to see a specific implementation approach for the snap-to-grid feature?


==========

Connections

A node can have an exit in up to 6 directions, north, east, south, west, up, down.  Nodes have 8 connection points: n, e, s, and w are in the middle of the node's border.  Up and down each have two points, one in each corner on the n border for up and one on each corner of the s border for down.  Only one exit (outgoing edge) is allowed for each direction.  That means that while there are two points each for up and down, only one each can be used.

You can create new nodes by dragging from a connection point and dropping the link on an empty space on the grid.  When creating a node this way, the incoming connection should be on the opposite side (outgoing north is incoming south, outgoing up is incoming down, etc.)  It works well for n, e, s, and w.  When creating a node by dragging from up ord down, the edge is always drawn from the e point at origin to the w point at the destination.   This is a bug.  The connection should always come from the point it was drawn from.  It should also go to the opposite point on the newly created node, so it can be properly displayed up or down and then a few spaces to the right or left, giving a diagonal layout.  Can you see what's gone wrong with up and down?




augment agent stuff

Connection refactoring with augment agent
You've outlined excellent principles for refactoring, and I completely agree with this disciplined approach. Let's apply these principles to create a step-by-step refactoring plan for the connection management code.
Refactoring Plan for Connection Management
Phase 1: Consolidate Connection Logic in ConnectionManager
Step 1: Enhance ConnectionManager with Missing Functionality
Add methods to ConnectionManager that handle all connection-related operations currently duplicated in ZoneCanvas
Keep existing code working while adding these methods
Test each new method individually
Step 2: Create Helper Methods for Coordinate Transformations
Add methods to handle coordinate system conversions consistently
These will be used by both the ConnectionManager and drawing code
Step 3: Refactor Connection Point Calculation
Move all connection point calculation logic from ZoneCanvas to ConnectionManager
Update ZoneCanvas to use the ConnectionManager methods instead of its own logic
Verify that the visual appearance remains unchanged
Phase 2: Simplify Connection Drawing in ZoneCanvas
Step 1: Extract Connection Drawing Logic
Create a separate function for drawing connections that uses ConnectionManager
Initially, this can be a direct extraction without changing logic
Verify that connections still display correctly
Step 2: Simplify Connection Drawing Logic
Gradually simplify the conditional logic in the drawing code
Replace special cases with more general patterns where possible
Test after each small change to ensure connections still display correctly
Step 3: Standardize Connection Representation
Update the connection data structure to better represent all connection types
Modify the drawing code to use this standardized representation
Ensure backward compatibility during the transition
Phase 3: Improve Connection Interaction Handling
Step 1: Extract Connection Interaction Logic
Move connection creation/deletion logic from ZoneCanvas to ConnectionManager
Update ZoneCanvas to delegate to ConnectionManager
Verify that all interactions still work as expected
Step 2: Simplify Connection Drag State
Refactor the ConnectionDragState to be more consistent and easier to understand
Update all code that uses this state
Test connection dragging thoroughly
Step 3: Clean Up Event Handling
Simplify the pointer input handling for connections
Make the event flow more predictable and easier to follow
Ensure all connection interactions still work correctly
Phase 4: Final Cleanup and Testing
Step 1: Remove Duplicated Code
Remove any remaining duplicated connection logic
Ensure all connection operations use the ConnectionManager
Step 2: Improve Naming and Documentation
Rename methods and variables for clarity
Add documentation to explain complex parts of the connection logic
Step 3: Comprehensive Testing
Test all connection operations to ensure they still work as expected
Verify that the visual appearance of connections is unchanged
Implementation Approach
For each step:
Make minimal, focused changes
Compile and test after each change
If a change breaks functionality, revert and try a smaller step
Only move to the next step when the current one is fully working
This approach follows your principles by:
Maintaining functionality throughout the process
Making incremental changes
Minimizing broken code
Not adding new features during refactoring
Are you comfortable with this plan? Would you like to adjust any aspects of it before we begin?
