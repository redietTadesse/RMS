export default function SpaceList({ spaces }) {
  return (
    <div className="space-list">
      <h2>Spaces</h2>
      <ul>
        {spaces?.map((space) => (
          <li key={space.id}>{space.name}</li>
        ))}
      </ul>
    </div>
  );
}
