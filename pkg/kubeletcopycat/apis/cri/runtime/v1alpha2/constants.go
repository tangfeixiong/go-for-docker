/*
  Inspired by
  - https://github.com/kubernetes/kubernetes/blob/master/pkg/kubelet/apis/cri/runtime/v1alpha2/constants.go
*/

package v1alpha2

// This file contains all constants defined in CRI.

// Required runtime condition type.
const (
	// RuntimeReady means the runtime is up and ready to accept basic containers.
	RuntimeReady = "RuntimeReady"
	// NetworkReady means the runtime network is up and ready to accept containers which require network.
	NetworkReady = "NetworkReady"
)

// LogStreamType is the type of the stream in CRI container log.
type LogStreamType string

const (
	// Stdout is the stream type for stdout.
	Stdout LogStreamType = "stdout"
	// Stderr is the stream type for stderr.
	Stderr LogStreamType = "stderr"
)

// LogTag is the tag of a log line in CRI container log.
// Currently defined log tags:
// * First tag: Partial/Full - P/F.
// The field in the container log format can be extended to include multiple
// tags by using a delimiter, but changes should be rare. If it becomes clear
// that better extensibility is desired, a more extensible format (e.g., json)
// should be adopted as a replacement and/or addition.
type LogTag string

const (
	// LogTagPartial means the line is part of multiple lines.
	LogTagPartial LogTag = "P"
	// LogTagFull means the line is a single full line or the end of multiple lines.
	LogTagFull LogTag = "F"
	// LogTagDelimiter is the delimiter for different log tags.
	LogTagDelimiter = ":"
)
