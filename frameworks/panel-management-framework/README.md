# Framework: Panels

Lets mods add their own panels, called **views**, to Bitwig's layout. Each view gets a button in the bottom bar, a **Show** action (shortcut, Commander) and a **scale**, and the framework puts it in its place. Mods can also give Bitwig's own panels a scale, and layout mods can rearrange Bitwig's panels.

On its own, the framework changes nothing in Bitwig: everything comes from the views, panels and layouts that mods register with it.

## Scale

A view, or one of Bitwig's panels a mod declares, can have its own scale: 75%, 100% (the default), 125%, 150%, 175% or 200%.

- **Where to set it:** in Settings (**Settings > User Interface > Panel Scale**, or wherever the mod puts the setting; see `scaleSetting` below), or right-click the panel's button in the bottom bar.
- **Where it applies:** the same setting wherever the panel is shown: as a mode's main content, as a lower-panel tab, as a panel beside the main area, as a tab of the browser side, or in a view stack or a side column.
- **When it applies:** at once, in every window.
- **How it scales:** relative to Bitwig's own UI scale, in quarter steps. With Bitwig at 100% every choice is exact; with Bitwig at 125%, a panel at 125% shows at 150%.
- **What follows it:** everything in the panel: text, icons, knobs and their hit areas. Menus, tooltips and pop-up windows opened from a scaled panel appear at Bitwig's own scale.
- **Where it is kept:** in the framework's settings (per install), never in projects.

Built against Bitwig 6.1.4 (Windows). Needs **Framework: Actions and Menus** and **Framework: Settings**.

## Where a view appears

**With Panel: Streamlined Views** (recommended), views are part of one **view stack** that replaces ARRANGE / MIX / EDIT in Single Large Monitor:
- Each view can be shown alone or added to the split. Click its button for the view alone; Cmd/Ctrl-click to add it to the split or remove it.
- Its size and visibility are saved with the project.
- Right-click a view's button for its scale and **Equalise View Sizes**.

**On the browser side**, if the mod asks for it (`side("browser")`, see [The browser side](#the-browser-side)), a view is a tab of the browser side or a column beside it, with or without a view stack.

**Without a view stack**, a view is one more tab of each mode's **lower panel** (or, in MIX, can take the mixer's place; see `replaces` below). In Single Large Monitor that is the panel under the arranger, mixer or Detail Editor that holds the Detail Editor, Devices and Mixer tabs.
- **Its button** sits in the bottom bar next to Bitwig's own buttons for that panel, after them. A click shows the view in the lower panel, or closes the panel if the view is already shown, as Bitwig's own tabs do.
- **Saved state:** each mode remembers its own lower panel, saved with the project.
- **Limits:**
  - Views are shown one at a time, like Bitwig's tabs. A view can't be shown alone or beside another view.
  - Display profiles and modes without such a lower panel don't show views: Tablet, Dual Monitor Arranger/Mixer, Triple Monitor, and some windows of the dual-monitor profiles. The log says so once per profile.

## Registering a view

As with the other frameworks: a registration in `mod.json`, a descriptor, and a provider class.

```json
"registrations": [
  { "id": "panels", "consumer": "panel-management-framework", "descriptor": "panels.properties", "jar": "helper.jar" }
],
"resources": {
  "my_icon": { "kind": "file", "path": "resources/my-mod/my-view.svg", "media_type": "image/svg+xml" }
}
```

`panels.properties`:

```properties
api.version=1
provider.class=com.example.mymod.MyPanels
```

The provider:

```java
import bml.panels.PanelProvider;
import bml.panels.Panels;

public final class MyPanels implements PanelProvider {
    @Override
    public void declare(Panels panels) {
        panels.view("my_view", "My View")        // view ID (unique in the mod), label
              .icon("my_icon")                    // required: a resource from mod.json
              .content(context -> MyWidget.create(context)) // required: one of Bitwig's widgets
              .savedText("notes", "")             // optional: a saved value of the view's own
              .scale(true);                       // a scale (default true)
    }
}
```

- **When it runs:** `declare` runs once, before the first display profile is built.
- **`.content(...)`** is called each time Bitwig builds a window's panel for the view, once for every place the view is in (each mode's tab, or the stack). It returns one of Bitwig's widgets (`dve`). The framework puts it in Bitwig's scaled container (Panel Scale) inside Bitwig's panel widget.
- **`ViewContext`:**
  - `window()`: the window's project view (`aBW`).
  - `shown()`: whether the view is shown there. In a stack this is its own "is shown"; as a tab it is true while attached.
  - `savedKey(key)`: the full key of a saved value, to read it from the window's view settings (`RDJ().OzW().cfM(key)`).
- **`.scale(boolean)`** (default true): whether the view has a scale. **`.scaleSetting(false)`** keeps the scale but leaves its setting to the mod: the framework lists it in no Settings section, and the mod places its own control with `bml.panels.PanelScale` (`choices()`, `defaultChoice()`, `get(modId, viewId)`, `set(modId, viewId, choice)`; a change applies at once, as from the framework's own setting).
- **`.replaces("mixer")`:** without a view stack, in a mode whose main content is the mixer (MIX), the view takes turns with the mixer as the main content instead of being a lower-panel tab there.
  - The mode's row in the bottom bar gets a Mixer and a view button, one shown at a time, the mixer by default.
  - Bitwig's own Toggle Mixer Panel chooses the mixer again.
  - The mixer keeps its saved settings, and the choice is saved per mode with the project.
  - Other modes keep the view as a lower-panel tab.
- **`.side("browser")`**, with **`.sideColumn(true)`** or **`.sideColumnShare(share)`:** the view goes on the browser side instead of the main area. See [The browser side](#the-browser-side).
- **`.savedText(key, default)`:** a text value kept with the project's view settings (per display profile), never in the project. There is one value per profile, shared by every place the view is in.
- **Icons:** the icon is required and is a resource from the mod's manifest.
  - `"kind": "bitwig"`: one of Bitwig's SVG icons, by its path under `icons/`. The framework checks that the file exists.
  - `"kind": "file"`: the mod's own SVG, 19 × 19, one colour on a transparent background.

  Bitwig tints the icon with the theme: only its shape counts. A view whose icon can't be used is logged and left out; there is no fallback icon.
- **Failures:** a view with an invalid ID, no label, no content or no usable icon is logged and left out. A provider that can't be loaded or throws in `declare` leaves out that mod's views. Other mods are unaffected.
- **Bitwig objects** come as `Object`, as in the other frameworks' APIs. A mod's code reaches Bitwig through the mod kit; the framework's own classes are translated separately, so they can't be extended across mods.

## The browser side

A view can go on the **browser side** instead of the main area: the tab group of the Browser, Project, Studio I/O and Mappings panels.

**No minimum width beside it.** Whenever the framework is loaded, the main area beside the browser side's tab group has no minimum width, on either side of the window and in every display profile, with or without views or layout mods. The browser side can be dragged as wide as the window allows, and a width it is given (by a drag, or by a mod) applies in full. The main area is then narrower, down to nothing; its content keeps its own minimum size and is cut off from its left edge, and the divider can still be dragged back to widen it. The window's own minimum width no longer includes the main area's.

```java
panels.view("my_view", "My View")
      .icon("my_icon")
      .content(context -> MyWidget.create(context))
      .side("browser")     // on the browser side, not in the main area
      .sideColumn(true);   // optional: a column of its own beside it, not a tab
                           // or .sideColumnShare(0.4): a column whose width is a share
```

**As a tab** (`side("browser")`):
- **Where:** one more tab of each window's browser side, after Bitwig's own tabs.
- **Its button** sits with theirs in the bottom bar, after them, and works as theirs do: a click shows the view on the browser side, or closes the side if the view is already shown there.
- **Show <label>** does what a click on the button does.
- **Saved state:** which tab the browser side shows is Bitwig's own choice, saved with the project's view settings.

**As a column** (`side("browser")` and `sideColumn(true)`):
- **Where:** a full-height column between the main area and the browser side. It is a side column (see `sideColumns` below), so it behaves as the other side columns do.
- **Width:** its own, taken from the main area when it is shown and given back when it is hidden. The width is saved with the project, as is whether it is shown. It is shown by default. Like every side column, it never makes the window wider.
- **Width as a share:** with `sideColumnShare(share)` in place of `sideColumn(true)`, the column's width is a share of the width it shares with the main area (the column and everything between it and the main area's far edge, not the browser side). `share` is above 0 and below 1: `0.4` starts the column at 40% and the main area at 60%. The column keeps its share when the window is resized, and dragging its divider changes the share. The share is saved with the project under a name of its own, so a width in pixels saved for the same view isn't read as a share. A share outside that range is logged and ignored. Everything else is as `sideColumn(true)`.
- **Its button** goes in the bottom bar's group with the browser side's buttons, in Bitwig's order (the order of the panels on screen), so it sits on the main-area side of them. The mods' column buttons sit together, set apart from the browser side's by one divider, as the On-Screen Keyboard's button is set apart, whichever side the browser side is on. A click shows the column alone, or hides it if it is already shown alone. Cmd/Ctrl-click adds it to the columns shown or removes it. Right-click gives its scale.
- **Actions:** **Show <label>** shows it alone. **Add or Remove <label> in the Split** adds or removes it.
- **When the browser side moves** to the other side of the window (Panel: Choose Sides), the column moves with it and stays beside it.

**Both ways:**
- **Nowhere else:** a browser-side view is not a tab of the modes' lower panels and not a view of the main view stack. `replaces` doesn't apply to it.
- **With Panel: Separate Browser Panels,** the browser side's panels are side columns, so each browser-side view is one more of those columns, after Bitwig's (towards the main area). A column view is shown by default; a tab view is hidden by default, as a tab not chosen. Separate Browser Panels changes only the first window of each display profile; other windows keep the tab group.
- **Several views** go in the views' order (see [Order](#order)).
- **Limits:** a window without a browser side doesn't show the view; the log says so once per profile. If the browser side isn't beside the main area in a left/right split, a column view becomes a tab there (logged).
- **Without the mod:** the view's saved values (its width, whether it is shown, the browser side's choice of tab) are view settings Bitwig doesn't know. A project saved with them still opens in Bitwig without mods.

## Layout policies (layout mods)

A layout mod, such as Panel: Streamlined Views, declares Bitwig panels it places itself and a layout policy:

```java
panels.bitwigView("tracks", "Tracks").icon("tracks_icon"); // one of Bitwig's panels, for the stack
panels.bitwigView("devices", "Device panel");              // outside the stack: only its scale
panels.policy(new MyLayout(), true);                        // a bml.panels.LayoutPolicy; true: it builds a view stack
```

**The order of work.** Only this framework patches Bitwig's display-profile builders. For every profile Bitwig builds, it runs, in order:
1. every policy's `layout(Layout)`, by mod ID;
2. the views: into the profile's stack if a policy made one, otherwise as tabs; then the browser-side views;
3. every policy's `arrange(Layout)`;
4. the side columns are put beside their edge, wherever `arrange` moved it.

**`Layout` gives the policy:**
- `profile()`.
- `bind(bitwigViewId, node)`: the panel node the policy built.
- `stack(name, bitwigViewIds...)`: builds the view stack, returning its layout node for the policy to put in place. It holds those Bitwig views in that order, then every registered view for the main area (not the browser-side views). One per profile; its buttons replace the mode chooser.
- `sideColumns(splitter, replaced, bitwigViewIds...)`: builds **side columns** in one of Bitwig's left/right splitters: those Bitwig panels side by side, each a full-height column with its own width. The first takes the place of `replaced` (one of the splitter's two parts) at the window's edge; the others follow towards the splitter's other part (the main area). Panel: Separate Browser Panels uses it for the Browser Panel, Project Panel, Studio Monitoring Panel and Mappings Panel, in place of the browser side's tab group. When the columns hold the Browser panel, the mods' browser-side views follow as more columns.
  - Showing a column takes width from the main area and hiding it gives the width back; the other columns keep theirs. Each column's width is saved with the project; the splitter's own saved width becomes the first column's, and the main area keeps its saved values. A column opened for the first time starts at the first column's width.
  - A column always opens when asked, and never makes the window wider: columns have no minimum width. The main area beside them has no minimum width either. When room runs out, the columns and the main area get narrower, down to nothing; nothing is closed. A panel's content, and the main area's, keeps its own minimum size and is cut off by a part narrower than it (the main area from its left edge). A narrow part's divider can still be dragged to widen it. While a window has side columns, its own minimum width no longer includes the main area's, so the window itself can be made narrower than Bitwig otherwise allows. The main area beside the browser side's own tab group, without columns, has no minimum width either (see [The browser side](#the-browser-side)).
  - Each panel keeps Bitwig's own button in the bottom bar, with the clicks of a view button: click shows it alone, a click on the panel already shown alone hides it, Cmd/Ctrl-click adds it or removes it, right-click gives its scale. Each column keeps its own width (there is no Equalise for columns). All may be hidden.
  - A panel is in one stack or set of columns at most.
  - If a policy's `arrange` moves the first column to the other side of the splitter (as Panel: Choose Sides does), the framework moves the other columns with it, so they stay beside it. It does the same when the windows are rebuilt after such a move.
- `showOnly(...)`: for presets.
- `isStackView(node)`: whether the node is in one of the profile's stacks.

## Scaling Bitwig's own panels

Any mod can give one of Bitwig's panels a scale, matched by the panel's class wherever Bitwig shows it:

```java
panels.bitwigPanel("tracks", "Tracks", bitwig.obf.ajv.class) // ID, label, a subclass of Bitwig's panel (akQ)
      .scaleSetting(false);                                  // optional: the mod places the setting itself
```

The panel gets Bitwig's scaled container, a right-click scale menu on its button and, unless `scaleSetting(false)`, a setting in the framework's Panel Scale section. A panel matching several declarations follows the first, by mod ID and then declaration order; a Bitwig view a layout mod binds with a scale of its own comes first. Panel: Scale Each Panel uses this for Tracks, the Detail Editor, the Mixer, the device panel, the Inspector, the four browser-side panels and the On-Screen Keyboard.

**Stack paths.** Each view's saved path in a stack is flat (`<stack>.<view>`), and each divider is named by the view above it. Adding or removing a mod's view moves nothing else's saved values.

**`viewStack`.** Bitwig registers actions before it builds any display profile, so the policy says up front whether it builds a view stack (`Layout.stack`). Only then do the mods' views get **Add or Remove ... in the Split** and is there an **Equalise View Sizes** action. A policy that only moves panels or builds side columns passes `false`. The older `panels.policy(policy)` is deprecated: it counts as building a stack when the mod declares a Bitwig view with an icon.

**Optional policy callbacks:**
- `nothingShown`: a project opens with no stack view shown.
- `showAlone`: a view of the policy's own is to be shown alone. Streamlined Views does this by choosing its mode.

**For a layout mod's own hooks:** `bml.panels.Stacks.layout(profile)` returns the profile's `Layout`, and `Stacks.scaleMenu(button, modId, viewId)` gives a button the scale menu.

## Arranging your own views

A mod that builds layouts of its own (a mode of its own, another window's layout, a view shown full screen) can place its views there itself, with `bml.panels.Views`:

- **`Views.newNode(modId, viewId)`:** a new panel node (`akQ`) of one of the mod's declared views, to put where Bitwig would put a panel.
  - Every call gives a new node, since each place in a layout needs its own.
  - The node is a plain panel, as the view is as a tab: it has no "is shown" of its own and shows wherever it is placed.
  - It gets the view's content (built for each window that shows it), icon, label, button and scale. The view's saved values (`savedText`) are one per display profile, shared by all its nodes there.
  - Its button's click shows it in the tab group holding it, as the framework's tabs do. Anywhere else, the mod decides how it is shown. The view's **Show** action doesn't act on it.
  - Call it while a display profile is built: from a layout policy, or from the mod's own hook on profile building.
  - It returns null (logged) when the mod declared no such view or the view was left out.
- **`Views.nodes(window, modId, viewId)`:** the nodes the view has in a window (`aBW`) now, as a list.
  - It includes the nodes outside the window's main areas (on the browser side, in side columns or in a view stack) and those in the window's current mode, whether the framework placed them or the mod did.
  - Each window, and each mode, has nodes of its own.
  - The list is in layout order: each splitter's first part (left or top) comes before its second. A mod that put the view in several places tells them apart by their parents (`uva()`).
  - It is empty when the view isn't in the window's layout now.
  - A node's "is shown" in a window, where it has one (a side column or a stack view), is `node.cfM(window.RDJ())`. A side column's width (its share, with `sideColumnShare`) is its parent splitter's saved position in the same view settings.
- **`Views.icon(modId, resourceId)`:** the icon name a window loads for one of the mod's icon resources, for a button the mod builds itself. The resource follows the rules for a view's icon: a `"kind": "bitwig"` resource gives that Bitwig icon's name, and the mod's own SVG gives a name the framework loads from the mod's file, tinted as Bitwig's icons are. It returns null (logged) when the resource can't be used.

## What every view gets

- **Its button** in the bottom bar, in Bitwig's own style, lit while the view is shown. Right-click it to choose the view's scale; in a stack, also **Equalise View Sizes**.
- **Actions** in Bitwig's Panel Management category. In IDs, the mod ID has hyphens turned into underscores.
  - **Show <label>**, `<mod_id>_show_<view_id>`. In a stack (or side columns) it shows the view alone; as a tab it does what a click on the button does, in the current mode or on the browser side.
  - **Add or Remove <label> in the Split**, `<mod_id>_split_<view_id>`, and once **Equalise View Sizes**, `panel_management_framework_equalise`. For the mods' views and Equalise, these exist only when a layout mod that builds a view stack is on (`viewStack`); a browser-side view gets the split action only as a column. A layout mod's own Bitwig views get Show and the split action whenever that mod is on, since they are in its stack or side columns.
- **Its scale:** in **Settings > User Interface > Panel Scale**, one setting per view labelled with the view's label (unless the mod places it itself, `scaleSetting(false)`), and in the right-click menu. A layout mod's Bitwig views follow a panel declared by class (for example by Panel: Scale Each Panel) unless the mod gives them a scale of their own.

## Order

The framework decides only where views go.
- **In a lower panel:** after Bitwig's own tabs.
- **On the browser side:** after Bitwig's own tabs; as columns, after Bitwig's panels, towards the main area.
- **In a stack:** after the layout mod's own Bitwig views, in the order it gives them.
- **In Settings:** Panel Scale is a section after Bitwig's own on the User Interface page, only while something is listed in it: Bitwig panels declared by class first, then layout mods' Bitwig views with a scale of their own, then the views.

Among themselves, views are alphabetical by label, then by mod ID and view ID.
