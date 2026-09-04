export default function TenantList({ tenants }) {
  return (
    <div className="tenant-list">
      <h2>Tenants</h2>
      <ul>
        {tenants?.map((tenant) => (
          <li key={tenant.id}>{tenant.name}</li>
        ))}
      </ul>
    </div>
  );
}
