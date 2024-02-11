# Aten Binding

The Aten Binding supports [Aten HDMI matrix switches](https://www.aten.com/de/de/products/professionelles-audiovideo/grafik-matrix-switches/). It allows you to switch input signals of available HDMI output channels and control mute and video status of the channels.

Currently, only telnet connection is supported, yet. Serial connection is under development and might be included in the future.

## Supported Things

- `hdmiMatrixSwitch`: A single HDMI matrix switch.

## Discovery

Auto discovery is not supported.

## Thing Configuration

### `hdmiMatrixSwitch` Thing Configuration

| Name                 | Type    | Description                                       | Default | Required | Advanced |
|----------------------|---------|---------------------------------------------------|---------|----------|----------|
| hostname             | text    | Hostname or IP address of the device              | N/A     | yes      | no       |
| username             | text    | Username to access the device                     | N/A     | yes      | no       |
| password             | text    | Password to access the device                     | N/A     | yes      | no       |
| numberOfInputSources | text    | Number of available input channels                | N/A     | yes      | no       |
| numberOfOutputZones  | text    | Number of available output channels               | N/A     | yes      | no       |
| refreshInterval      | integer | Interval the device is polled in sec.             | 60      | no       | yes      |
| inputNames           | text    | Comma separated list of names for the input zones | N/A     | no       | yes      |
| profileNames         | text    | Comma separated list of names for the profiles    | N/A     | no       | yes      |
| telnetPort           | integer | Port number for telnet connection                 | 23      | no       | yes      |

## Channels

For the switch it's possible to define profiles with predefined sources for every channel.

| Channel  | Type   | Description                                                                           |
|----------|--------|---------------------------------------------------------------------------------------|
| profile  | String | Profile selection as configured in the switch (for names, see profileNames parameter) |

The channels are automatically generated for the configured number of output zones.

| Channel  | Type   | Description                                                               |
|----------|--------|---------------------------------------------------------------------------|
| input    | String | Source selection for the output zone (for names, see inputNames parameter)|
| mute     | Switch | Mutes the zone                                                            |
| audio    | Switch | Enable/disable audio signal for the zone (=inverted mute state)           |
| video    | Switch | Enable/disable the video signal for the zone                              |
| cec      | Switch | Enable/disable consumer electronics control (cec) of connected TV         |

## Full Example

_TBD_

