export function Toggle({ on, onClick, locked }: { on: boolean; onClick?: () => void; locked?: boolean }) {
  return (
    <button className={`idt-toggle ${on ? "on" : ""} ${locked ? "locked" : ""}`} onClick={onClick} disabled={locked} aria-pressed={on}>
      <span className="knob" />
    </button>
  );
}
