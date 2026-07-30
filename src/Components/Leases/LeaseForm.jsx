import React from 'react';

export default function LeaseForm() {
  return (
    <form className="lease-form">
      <h2>Add Lease</h2>
      <label>
        Property
        <input type="text" name="property" />
      </label>
      <label>
        Tenant
        <input type="text" name="tenant" />
      </label>
      <button type="submit">Save Lease</button>
    </form>
  );
}
