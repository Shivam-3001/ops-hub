# Hierarchy Filter Component Documentation

## Overview

A cascading hierarchical filter component for filtering data by Cluster → Circle → Zone → Area.

## Features

✅ **Cascading Dropdowns** - Selecting a parent automatically loads children
✅ **Loading States** - Visual feedback while data loads
✅ **Error Handling** - Displays errors gracefully
✅ **Reset Functionality** - Clear all filters with one click
✅ **Active Filters Display** - Shows selected filters with remove buttons
✅ **Responsive Design** - Works on all screen sizes
✅ **Disabled States** - Prevents invalid selections

## Component Location

`src/components/HierarchyFilter.js`

## Usage

### Basic Usage

```jsx
import HierarchyFilter from "@/components/HierarchyFilter";

function MyPage() {
  return (
    <div>
      <HierarchyFilter 
        onFilterChange={(filters) => {
          console.log("Filters:", filters);
        }} 
      />
    </div>
  );
}
```

### With Filter Handler

```jsx
import HierarchyFilter from "@/components/HierarchyFilter";
import { useState } from "react";

function MyPage() {
  const [filters, setFilters] = useState(null);

  const handleFilterChange = (selectedFilters) => {
    setFilters(selectedFilters);
    // Use filters to query data, update charts, etc.
    console.log("Cluster:", selectedFilters.cluster);
    console.log("Circle:", selectedFilters.circle);
    console.log("Zone:", selectedFilters.zone);
    console.log("Area:", selectedFilters.area);
  };

  return (
    <div>
      <HierarchyFilter onFilterChange={handleFilterChange} />
      {filters && (
        <div>
          <p>Filtering by: {JSON.stringify(filters)}</p>
        </div>
      )}
    </div>
  );
}
```

## Props

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `onFilterChange` | Function | No | Callback function that receives filter object `{ cluster, circle, zone, area }` |
| `className` | String | No | Additional CSS classes for styling |

## Filter Object Structure

The `onFilterChange` callback receives an object with the following structure:

```javascript
{
  cluster: Number | null,  // Selected cluster ID or null
  circle: Number | null,   // Selected circle ID or null
  zone: Number | null,     // Selected zone ID or null
  area: Number | null      // Selected area ID or null
}
```

## API Integration

The component uses the API service from `@/lib/api`:

- `api.getClusters()` - Loads all clusters
- `api.getCircles(clusterId)` - Loads circles for a cluster
- `api.getZones(circleId)` - Loads zones for a circle
- `api.getAreas(zoneId)` - Loads areas for a zone

## UI Features

1. **Cascading Behavior**
   - Select Cluster → Circles load
   - Select Circle → Zones load
   - Select Zone → Areas load
   - Clearing parent clears children

2. **Visual Feedback**
   - Loading indicators for each dropdown
   - Disabled state when parent not selected
   - Error messages displayed at top

3. **Active Filters**
   - Tag-style display of selected filters
   - Individual remove buttons (×) on each tag
   - Color-coded by hierarchy level

4. **Reset Button**
   - Appears when any filter is selected
   - Clears all selections at once

## Integration in Dashboard

The filter is already integrated into the dashboard page at `/dashboard`. It appears at the top of the main content area.

## Example: Filtering Data

```jsx
const [filteredData, setFilteredData] = useState([]);

const handleFilterChange = async (filters) => {
  // Build query parameters
  const params = new URLSearchParams();
  if (filters.cluster) params.append('clusterId', filters.cluster);
  if (filters.circle) params.append('circleId', filters.circle);
  if (filters.zone) params.append('zoneId', filters.zone);
  if (filters.area) params.append('areaId', filters.area);

  // Fetch filtered data
  const response = await fetch(`/api/data?${params}`);
  const data = await response.json();
  setFilteredData(data);
};

<HierarchyFilter onFilterChange={handleFilterChange} />
```

## Styling

The component uses Tailwind CSS classes and follows the application's design system:
- Slate color palette
- Rounded corners (rounded-lg)
- Consistent spacing and padding
- Hover states and transitions

## Error Handling

Errors are displayed in a red banner at the top of the component. The component continues to function even if one level fails to load.

## Next Steps

- Add filter persistence (localStorage)
- Add URL query parameter sync
- Add debouncing for rapid selections
- Add filter counts (e.g., "12 areas found")
