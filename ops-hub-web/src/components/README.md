# Components

This directory contains reusable React components.

## HierarchyFilter

A cascading filter component for hierarchical data filtering (Cluster → Circle → Zone → Area).

### Usage

```jsx
import HierarchyFilter from "@/components/HierarchyFilter";

function MyComponent() {
  const handleFilterChange = (filters) => {
    console.log("Selected filters:", filters);
    // filters = { cluster, circle, zone, area }
  };

  return (
    <HierarchyFilter onFilterChange={handleFilterChange} />
  );
}
```

### Props

- `onFilterChange` (function, optional): Callback function that receives the selected filter values
  - Parameters: `{ cluster, circle, zone, area }` - all can be null or numbers
- `className` (string, optional): Additional CSS classes

### Features

- Cascading dropdowns (selecting a parent loads children)
- Loading states for each dropdown
- Error handling
- Reset/Clear filters button
- Active filters display with remove buttons
- Disabled state when parent not selected
