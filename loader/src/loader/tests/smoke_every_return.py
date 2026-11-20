"""returns: all (the *-all commands of bitwig-mod-preparer.jar): the bridge runs at every return.

Run from an install (or a release folder): python loader/tests/smoke_every_return.py. Needs javac and java on PATH.
A class with several returns per method (instance and static, int/long/double/object parameters, object and void
returns) is patched, then run with -Xverify:all; every return must come back through the bridge, with the
parameters as passed in. Invalid hooks (a local that isn't an object parameter, a constructor) must be refused.
"""
from pathlib import Path
import shutil, subprocess, tempfile

S = Path(__file__).resolve().parents[2]
TOOL = S / "loader/tools/bitwig-mod-preparer.jar"
T = Path(tempfile.mkdtemp(prefix="bml-every-return-"))
src = T / "src/t"; src.mkdir(parents=True)
(src / "Multi.java").write_text("""package t;
import java.util.List;
public class Multi {
    public String pick(int i, long l, double d, String s) {
        if (i == 0) return "a";
        if (l == 1L) return "b";
        switch (i) { case 5: return "five"; case 6: return "six"; default: break; }
        if (d > 0) { s = "reassigned"; return "d"; }
        return "end";
    }
    public static Object spick(int i, Object o) { if (i == 0) return "s0"; return "s1"; }
    public void log(int i, List<String> out) { if (i == 0) return; out.add("body"); if (i == 1) return; out.add("more"); }
    public static void slog(List<String> out, int i) { if (i == 0) return; out.add("sbody"); }
}
""")
(src / "Bridge.java").write_text("""package t;
import java.util.List;
public class Bridge {
    public static Object wrap(Object r, Object self, Object s) { return r + "|" + (self != null) + "|" + s; }
    public static Object swrap(Object r, Object o) { return r + "|" + o; }
    @SuppressWarnings("unchecked") public static void after(Object self, Object out) { ((List<String>) out).add("after"); }
    @SuppressWarnings("unchecked") public static void safter(Object out) { ((List<String>) out).add("safter"); }
}
""")
(src / "Main.java").write_text("""package t;
import java.util.*;
public class Main { public static void main(String[] a) {
    Multi m = new Multi();
    System.out.println(m.pick(0,0,0,"x")+" "+m.pick(1,1,0,"x")+" "+m.pick(5,0,0,"x")+" "+m.pick(6,0,0,"x")+" "+m.pick(2,0,1,"x")+" "+m.pick(2,0,-1,"x"));
    System.out.println(Multi.spick(0,"o")+" "+Multi.spick(1,"o"));
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < 3; i++) { List<String> l = new ArrayList<>(); m.log(i, l); b.append(l).append(' '); }
    for (int i = 0; i < 2; i++) { List<String> l = new ArrayList<>(); Multi.slog(l, i); b.append(l).append(' '); }
    System.out.println(b.toString().trim());
}}
""")
classes = T / "classes"
subprocess.run(["javac", "--release", "21", "-d", str(classes), *map(str, src.glob("*.java"))], check=True)
OBJ = "Ljava/lang/Object;"


def tool(*args, ok=True):
    p = subprocess.run(["java", "-jar", str(TOOL), *map(str, args)], capture_output=True, text=True)
    assert (p.returncode == 0) == ok, (args, p.stdout, p.stderr)
    return p.stdout + p.stderr


out = T / "patched"; out.mkdir()
steps = [("patch-class-wrap-object-all", "pick", "(IJDLjava/lang/String;)Ljava/lang/String;", "wrap", f"({OBJ}{OBJ}{OBJ}){OBJ}", "this,arg6", 6),
         ("patch-class-wrap-object-all", "spick", f"(I{OBJ}){OBJ}", "swrap", f"({OBJ}{OBJ}){OBJ}", "arg1", 2),
         ("patch-class-final-all", "log", "(ILjava/util/List;)V", "after", f"({OBJ}{OBJ})V", "this,arg2", 3),
         ("patch-class-final-all", "slog", "(Ljava/util/List;I)V", "safter", f"({OBJ})V", "arg0", 2)]
current = classes / "t/Multi.class"
for n, (cmd, method, desc, bridge, bdesc, args, returns) in enumerate(steps):
    nxt = out / f"{n}.class"
    report = tool(cmd, current, nxt, method, desc, "t.Bridge", bridge, bdesc, args)
    assert f"returns=all" in report and f"returns_wrapped={returns} body_moved_to=bml$returns${method}" in report, report
    current = nxt
assert "is not an object parameter" in tool("patch-class-wrap-object-all", classes / "t/Multi.class", out / "x.class", "pick",
                                             "(IJDLjava/lang/String;)Ljava/lang/String;", "t.Bridge", "wrap", f"({OBJ}{OBJ}){OBJ}", "arg1", ok=False)
assert "can't hook <init>" in tool("patch-class-final-all", classes / "t/Multi.class", out / "x.class", "<init>", "()V",
                                   "t.Bridge", "safter", f"({OBJ})V", ok=False)
run = T / "run"; shutil.copytree(classes, run); shutil.copy(current, run / "t/Multi.class")
p = subprocess.run(["java", "-Xverify:all", "-cp", str(run), "t.Main"], capture_output=True, text=True)
assert p.returncode == 0, p.stderr
assert p.stdout.splitlines() == ["a|true|x b|true|x five|true|x six|true|x d|true|x end|true|x", "s0|o s1|o",
                                 "[after] [body, after] [body, more, after] [safter] [sbody, safter]"], p.stdout
shutil.rmtree(T, ignore_errors=True)
print("PASS: returns: all wraps every return (object and void, instance and static), parameters as passed in; invalid hooks refused")
