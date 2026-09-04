export default function SpaceForm() {
  return (
    <form className="space-form">
      <h2>Add Space</h2>
      <label>
        Name
        <input type="text" name="name" />
      </label>
      <label>
        Description
        <textarea name="description" />
      </label>
      <button type="submit">Save Space</button>
    </form>
  );
}
