import React from 'react';

export default function LeaseList({ leases }) {
  return (
    <div className="lease-list">
      <h2>Leases</h2>
      <ul>
        {leases?.map((lease) => (
          <li key={lease.id}>{lease.property} - {lease.tenant}</li>
        ))}
      </ul>
    </div>
  );
}
