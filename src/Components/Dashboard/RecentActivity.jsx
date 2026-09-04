export default function RecentActivity({ items }) {
  return (
    <section className="recent-activity">
      <h2>Recent Activity</h2>
      <ul>
        {items?.map((item, index) => (
          <li key={index}>{item}</li>
        ))}
      </ul>
    </section>
  );
}
