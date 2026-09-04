export default function TenantForm() {
  return (
    <form className="tenant-form">
      <h2>Add Tenant</h2>
      <label>
        Name
        <input type="text" name="name" />
      </label>
      <label>
        Email
        <input type="email" name="email" />
      </label>
      <button type="submit">Save Tenant</button>
    </form>
  );
}
