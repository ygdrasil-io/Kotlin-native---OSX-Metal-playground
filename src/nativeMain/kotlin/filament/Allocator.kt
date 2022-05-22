package filament

const val FILAMENT_MIN_COMMAND_BUFFERS_SIZE_IN_MB = 1
const val FILAMENT_PER_RENDER_PASS_ARENA_SIZE_IN_MB = 2
const val FILAMENT_PER_FRAME_COMMANDS_SIZE_IN_MB = 1

// per render pass allocations
// Froxelization needs about 1 MiB. Command buffer needs about 1 MiB.
const val CONFIG_PER_RENDER_PASS_ARENA_SIZE  = FILAMENT_PER_RENDER_PASS_ARENA_SIZE_IN_MB * 1024 * 1024;

// size of the high-level draw commands buffer (comes from the per-render pass allocator)
const val CONFIG_PER_FRAME_COMMANDS_SIZE     = FILAMENT_PER_FRAME_COMMANDS_SIZE_IN_MB * 1024 * 1024;

// size of a command-stream buffer (comes from mmap -- not the per-engine arena)
const val CONFIG_MIN_COMMAND_BUFFERS_SIZE    = FILAMENT_MIN_COMMAND_BUFFERS_SIZE_IN_MB * 1024 * 1024;
const val CONFIG_COMMAND_BUFFERS_SIZE        = 3 * CONFIG_MIN_COMMAND_BUFFERS_SIZE;