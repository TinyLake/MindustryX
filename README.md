![Logo](core/assets-raw/sprites/ui/logo.png)

### About MindustryX
Two Purpose:
* Add more useful apis to `core`, which can be use by mods/plugins.
* More features/optimizations for `server`.

All change should be compatible with official client.(can be optional with client support)  
This project will publish more frequently than the upstream.

### Build
This project will only release server builds.
Core api will be available in Jitpack.

If you'd rather compile on your own, `gradlew server:dist`.

### Commit Tag
* S: server only change.
* SO: server and optional client.
* SC: server and client all need to update, otherwise will disable.
* C: client only change.
* API: no effect, pure api for mod/plugin.
* fix: fix bug of previous feature
* None tag means not game change.

### Features
- [*] S: update TypeIO.writeRequest, not write config
- [*] API: add SendPacketEvent
- [*] S: Add icons.properties,bundles,block_colors.png to server build
- [*] S: Load icons,locales in server launcher
- [ ] API: add DamageSource and DamageEvent
- [ ] C: bind `usid` to hostname rather than ip
- ~~[*] API: add DropDownEvent~~ (Merge to master as `PayloadDropEvent`)
- ~~[ ] S: ipForward support~~ (consider remove)
- ~~[ ] SO: new GameRule, core resource isolation~~ (consider remove)